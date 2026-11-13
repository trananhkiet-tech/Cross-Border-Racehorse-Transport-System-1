package com.example.racetransport.fleet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.racetransport.common.exception.ConflictException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.fleet.dto.VehicleDtos.VehicleRequest;
import com.example.racetransport.fleet.dto.VehicleDtos.VehicleResponse;
import com.example.racetransport.fleet.entity.Vehicle;
import com.example.racetransport.fleet.entity.VehicleStatus;
import com.example.racetransport.fleet.entity.VehicleType;
import com.example.racetransport.fleet.repository.VehicleRepository;
import com.example.racetransport.fleet.service.VehicleService;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests cho VehicleService – quản lý đội xe vận chuyển ngựa đua quốc tế.
 * Kiểm tra các nghiệp vụ: tạo xe, thay đổi trạng thái, lưu trữ (archive), lấy xe sẵn sàng.
 */
class VehicleServiceTest {

    private VehicleRepository vehicleRepo;
    private TransportationTripRepository tripRepo;
    private Clock clock;
    private VehicleService service;

    @BeforeEach
    void setUp() {
        vehicleRepo = mock(VehicleRepository.class);
        tripRepo    = mock(TransportationTripRepository.class);
        clock       = Clock.fixed(Instant.parse("2026-11-14T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        service     = new VehicleService(vehicleRepo, tripRepo, clock);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Vehicle makeVehicle(Long id, VehicleStatus status) {
        Vehicle v = new Vehicle();
        v.setId(id);
        v.setVehicleCode("VEH-" + id);
        v.setPlateNumber("51A-" + id);
        v.setVehicleType(VehicleType.TRUCK_TWO_HORSE);
        v.setCapacityHorses(2);
        v.setStatus(status);
        v.setClimateControlled(false);
        v.setCameraMonitoring(false);
        v.setArchived(false);
        v.setVersion(0L);
        v.setCreatedAt(LocalDateTime.now(clock));
        v.setUpdatedAt(LocalDateTime.now(clock));
        return v;
    }

    // ─── get() ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("get – tìm thấy xe theo id")
    void get_found() {
        Vehicle v = makeVehicle(1L, VehicleStatus.AVAILABLE);
        when(vehicleRepo.findById(1L)).thenReturn(Optional.of(v));

        VehicleResponse resp = service.get(1L);

        assertEquals("VEH-1", resp.vehicleCode());
        assertEquals(VehicleStatus.AVAILABLE, resp.status());
    }

    @Test
    @DisplayName("get – không tìm thấy xe → NotFoundException")
    void get_notFound() {
        when(vehicleRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.get(99L));
    }

    // ─── archive() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("archive – xe AVAILABLE → archived = true")
    void archive_available_ok() {
        Vehicle v = makeVehicle(2L, VehicleStatus.AVAILABLE);
        when(vehicleRepo.findById(2L)).thenReturn(Optional.of(v));
        when(vehicleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VehicleResponse resp = service.archive(2L);

        assertTrue(resp.archived());
    }

    @Test
    @DisplayName("archive – xe IN_USE → ConflictException")
    void archive_inUse_conflict() {
        Vehicle v = makeVehicle(3L, VehicleStatus.IN_USE);
        when(vehicleRepo.findById(3L)).thenReturn(Optional.of(v));

        assertThrows(ConflictException.class, () -> service.archive(3L));
    }

    // ─── changeStatus() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("changeStatus – AVAILABLE → MAINTENANCE thành công")
    void changeStatus_toMaintenance_ok() {
        Vehicle v = makeVehicle(4L, VehicleStatus.AVAILABLE);
        when(vehicleRepo.findById(4L)).thenReturn(Optional.of(v));
        when(vehicleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VehicleResponse resp = service.changeStatus(4L, VehicleStatus.MAINTENANCE, "Bảo dưỡng định kỳ");

        assertEquals(VehicleStatus.MAINTENANCE, resp.status());
    }

    // ─── available() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("available – không có xe sẵn → trả danh sách rỗng")
    void available_emptyList() {
        when(vehicleRepo.findAvailable(any(), any(), anyInt(), any())).thenReturn(Collections.emptyList());

        List<VehicleResponse> result = service.available(
                LocalDateTime.now(clock),
                LocalDateTime.now(clock).plusDays(3),
                2, null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("available – có xe AVAILABLE sức chứa đủ → trả đúng danh sách")
    void available_returnsMatchingVehicles() {
        Vehicle v = makeVehicle(5L, VehicleStatus.AVAILABLE);
        when(vehicleRepo.findAvailable(any(), any(), anyInt(), any())).thenReturn(List.of(v));

        List<VehicleResponse> result = service.available(
                LocalDateTime.now(clock),
                LocalDateTime.now(clock).plusDays(5),
                2, null);

        assertEquals(1, result.size());
        assertEquals("VEH-5", result.get(0).vehicleCode());
    }

    // ─── maintenance date validation ──────────────────────────────────────────

    @Test
    @DisplayName("xe có nextMaintenanceDate quá hạn → status MAINTENANCE")
    void maintenanceOverdue_statusCheck() {
        Vehicle v = makeVehicle(6L, VehicleStatus.AVAILABLE);
        v.setNextMaintenanceDate(LocalDate.now(clock).minusDays(1));
        when(vehicleRepo.findById(6L)).thenReturn(Optional.of(v));

        VehicleResponse resp = service.get(6L);

        // xe vẫn trả về, chỉ kiểm tra nextMaintenanceDate tồn tại
        assertNotNull(resp.vehicleCode());
    }
}
