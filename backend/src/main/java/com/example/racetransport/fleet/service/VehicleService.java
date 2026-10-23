package com.example.racetransport.fleet.service;

import com.example.racetransport.common.exception.ConflictException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.fleet.dto.VehicleDtos.VehicleRequest;
import com.example.racetransport.fleet.dto.VehicleDtos.VehicleResponse;
import com.example.racetransport.fleet.dto.VehicleDtos.VehicleTripRef;
import com.example.racetransport.fleet.entity.Vehicle;
import com.example.racetransport.fleet.entity.VehicleStatus;
import com.example.racetransport.fleet.entity.VehicleType;
import com.example.racetransport.fleet.repository.VehicleRepository;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quản lý đội xe: phương tiện, trạng thái sẵn sàng, bảo dưỡng. Lưu trữ thay vì xóa. */
@Service
public class VehicleService {

    /** Trạng thái chuyến coi là "còn hiệu lực" (chưa kết thúc). */
    public static final EnumSet<TripStatus> OPEN_TRIP = EnumSet.complementOf(
            EnumSet.of(TripStatus.DELIVERED, TripStatus.CANCELLED));

    private final VehicleRepository vehicles;
    private final TransportationTripRepository trips;
    private final Clock clock;

    public VehicleService(VehicleRepository vehicles, TransportationTripRepository trips, Clock clock) {
        this.vehicles = vehicles;
        this.trips = trips;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> list(Collection<VehicleStatus> statuses, Collection<VehicleType> types, String q) {
        return vehicles.search(statuses == null || statuses.isEmpty() ? EnumSet.allOf(VehicleStatus.class) : statuses,
                        types == null || types.isEmpty() ? EnumSet.allOf(VehicleType.class) : types,
                        q == null || q.isBlank() ? null : q.trim())
                .stream().map(v -> toResponse(v, false)).toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse get(Long id) {
        return toResponse(load(id), true);
    }

    /** Xe sẵn sàng cho khung giờ [from, to] và đủ sức chứa. */
    @Transactional(readOnly = true)
    public List<VehicleResponse> available(LocalDateTime from, LocalDateTime to, int minCapacity, Long excludeTripId) {
        Guard.input(from != null && to != null && to.isAfter(from), "Khung giờ không hợp lệ");
        return vehicles.search(EnumSet.of(VehicleStatus.AVAILABLE, VehicleStatus.IN_USE),
                        EnumSet.allOf(VehicleType.class), null).stream()
                .filter(v -> v.getCapacityHorses() >= minCapacity)
                .filter(v -> !trips.existsVehicleOverlap(v.getId(), excludeTripId == null ? -1L : excludeTripId, from, to))
                .map(v -> toResponse(v, false)).toList();
    }

    @Transactional
    public VehicleResponse create(VehicleRequest req) {
        if (vehicles.existsByVehicleCodeIgnoreCase(req.vehicleCode())) {
            throw new ConflictException("Mã phương tiện đã tồn tại");
        }
        if (vehicles.existsByPlateNumberIgnoreCase(req.plateNumber())) {
            throw new ConflictException("Biển số đã tồn tại");
        }
        Vehicle v = new Vehicle();
        v.setStatus(VehicleStatus.AVAILABLE);
        apply(v, req);
        vehicles.save(v);
        return toResponse(v, true);
    }

    @Transactional
    public VehicleResponse update(Long id, VehicleRequest req) {
        Vehicle v = load(id);
        Guard.state(!Boolean.TRUE.equals(v.getArchived()), "Phương tiện đã lưu trữ");
        if (vehicles.existsByVehicleCodeIgnoreCaseAndIdNot(req.vehicleCode(), id)) {
            throw new ConflictException("Mã phương tiện đã tồn tại");
        }
        if (vehicles.existsByPlateNumberIgnoreCaseAndIdNot(req.plateNumber(), id)) {
            throw new ConflictException("Biển số đã tồn tại");
        }
        apply(v, req);
        return toResponse(v, true);
    }

    /** Đổi trạng thái vận hành: AVAILABLE / MAINTENANCE / UNAVAILABLE. Không đổi khi xe đang chạy chuyến. */
    @Transactional
    public VehicleResponse changeStatus(Long id, VehicleStatus status, String note) {
        Vehicle v = load(id);
        Guard.input(EnumSet.of(VehicleStatus.AVAILABLE, VehicleStatus.MAINTENANCE, VehicleStatus.UNAVAILABLE)
                .contains(status), "Chỉ đặt Sẵn sàng / Bảo dưỡng / Không khả dụng");
        Guard.state(v.getStatus() != VehicleStatus.IN_USE, "Xe đang thực hiện chuyến, không thể đổi trạng thái");
        Guard.state(v.getStatus() != VehicleStatus.RETIRED, "Xe đã ngừng sử dụng");
        if (status != VehicleStatus.AVAILABLE) {
            Guard.state(trips.countByVehicleIdAndStatusIn(v.getId(), EnumSet.of(TripStatus.SCHEDULED,
                            TripStatus.PREPARING, TripStatus.READY_FOR_DEPARTURE)) == 0,
                    "Xe đang được phân công cho chuyến sắp chạy; hãy đổi xe cho chuyến trước");
        }
        VehicleStatus previous = v.getStatus();
        v.setStatus(status);
        if (note != null && !note.isBlank()) {
            v.setNotes(note);
        }
        if (status == VehicleStatus.AVAILABLE && previous == VehicleStatus.MAINTENANCE) {
            v.setLastMaintenanceDate(LocalDate.now(clock));
        }
        return toResponse(v, true);
    }

    @Transactional
    public VehicleResponse archive(Long id) {
        Vehicle v = load(id);
        Guard.state(trips.countByVehicleIdAndStatusIn(id, OPEN_TRIP) == 0, "Xe còn chuyến chưa hoàn tất");
        v.setArchived(true);
        v.setStatus(VehicleStatus.RETIRED);
        return toResponse(v, false);
    }

    public Vehicle load(Long id) {
        return vehicles.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy phương tiện"));
    }

    private void apply(Vehicle v, VehicleRequest req) {
        v.setVehicleCode(req.vehicleCode().trim().toUpperCase());
        v.setPlateNumber(req.plateNumber().trim().toUpperCase());
        v.setVehicleType(req.vehicleType());
        v.setModel(req.model());
        v.setCapacityHorses(req.capacityHorses());
        v.setCurrentLocation(req.currentLocation());
        v.setClimateControlled(Boolean.TRUE.equals(req.climateControlled()));
        v.setCameraMonitoring(Boolean.TRUE.equals(req.cameraMonitoring()));
        v.setLastMaintenanceDate(req.lastMaintenanceDate());
        if (req.nextMaintenanceDate() != null && req.lastMaintenanceDate() != null) {
            Guard.input(req.nextMaintenanceDate().isAfter(req.lastMaintenanceDate()),
                    "Ngày bảo dưỡng kế tiếp phải sau ngày bảo dưỡng gần nhất");
        }
        v.setNextMaintenanceDate(req.nextMaintenanceDate());
        v.setNotes(req.notes());
    }

    public VehicleResponse toResponse(Vehicle v, boolean withTrips) {
        List<VehicleTripRef> upcoming = withTrips ? trips.findByVehicleIdOrderByPlannedDepartureDesc(v.getId()).stream()
                .filter(t -> OPEN_TRIP.contains(t.getStatus()))
                .map(t -> new VehicleTripRef(t.getId(), t.getTripNumber(), t.getStatus(), t.getPlannedDeparture(),
                        t.getPlannedArrival(), t.getRoute().getName()))
                .toList() : List.of();
        boolean due = v.getNextMaintenanceDate() != null
                && !v.getNextMaintenanceDate().isAfter(LocalDate.now(clock).plusDays(7));
        return new VehicleResponse(v.getId(), v.getVehicleCode(), v.getPlateNumber(), v.getVehicleType(),
                v.getModel(), v.getCapacityHorses(), v.getStatus(), v.getCurrentLocation(),
                Boolean.TRUE.equals(v.getClimateControlled()), Boolean.TRUE.equals(v.getCameraMonitoring()),
                v.getLastMaintenanceDate(), v.getNextMaintenanceDate(), due, v.getNotes(),
                Boolean.TRUE.equals(v.getArchived()), v.getUpdatedAt(), upcoming);
    }
}
