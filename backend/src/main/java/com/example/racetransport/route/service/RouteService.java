package com.example.racetransport.route.service;

import com.example.racetransport.common.exception.ConflictException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.common.util.Countries;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.route.dto.RouteDtos.RouteDetail;
import com.example.racetransport.route.dto.RouteDtos.RouteRequest;
import com.example.racetransport.route.dto.RouteDtos.RouteStopRequest;
import com.example.racetransport.route.dto.RouteDtos.RouteStopResponse;
import com.example.racetransport.route.dto.RouteDtos.RouteSummary;
import com.example.racetransport.route.entity.Route;
import com.example.racetransport.route.entity.RouteStatus;
import com.example.racetransport.route.entity.RouteStop;
import com.example.racetransport.route.repository.RouteRepository;
import com.example.racetransport.route.repository.RouteStopRepository;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tuyến đường mẫu và điểm dừng (pickup, rest stop, sân bay, cửa khẩu, hải quan, kiểm dịch, điểm đến).
 * Bản đồ hiển thị bằng sơ đồ minh họa phía frontend, không cần API key.
 */
@Service
public class RouteService {

    /** Trạng thái chuyến đang chạy – không cho sửa điểm dừng của tuyến lúc này. */
    private static final EnumSet<TripStatus> RUNNING = EnumSet.of(TripStatus.IN_TRANSIT, TripStatus.AT_CHECKPOINT,
            TripStatus.AT_BORDER, TripStatus.CUSTOMS_CLEARANCE, TripStatus.QUARANTINE, TripStatus.DELAYED);

    private final RouteRepository routes;
    private final RouteStopRepository stops;
    private final TransportationTripRepository trips;
    private final CodeGenerator codes;

    public RouteService(RouteRepository routes, RouteStopRepository stops, TransportationTripRepository trips,
                        CodeGenerator codes) {
        this.routes = routes;
        this.stops = stops;
        this.trips = trips;
        this.codes = codes;
    }

    @Transactional(readOnly = true)
    public List<RouteSummary> list(Collection<RouteStatus> statuses, String origin, String destination, String q) {
        List<Route> list = routes.search(statuses == null || statuses.isEmpty() ? EnumSet.allOf(RouteStatus.class)
                        : statuses, blank(origin), blank(destination), blank(q));
        Map<Long, Long> counts = list.isEmpty() ? Map.of()
                : stops.findByRouteIdIn(list.stream().map(Route::getId).toList()).stream()
                .collect(Collectors.groupingBy(s -> s.getRoute().getId(), Collectors.counting()));
        return list.stream().map(r -> toSummary(r, counts.getOrDefault(r.getId(), 0L).intValue())).toList();
    }

    @Transactional(readOnly = true)
    public RouteDetail get(Long id) {
        return toDetail(load(id));
    }

    @Transactional
    public RouteDetail create(RouteRequest req) {
        validate(req);
        Route r = new Route();
        String code = blank(req.routeCode());
        if (code == null) {
            code = codes.next(CodeGenerator.ROUTE);
        } else if (routes.existsByRouteCode(code)) {
            throw new ConflictException("Mã tuyến đã tồn tại");
        }
        r.setRouteCode(code);
        apply(r, req);
        routes.save(r);
        if (req.stops() != null) {
            int order = 1;
            for (RouteStopRequest s : req.stops()) {
                RouteStop stop = new RouteStop();
                stop.setRoute(r);
                applyStop(stop, s);
                stop.setStopOrder(s.stopOrder() == null ? order : s.stopOrder());
                Guard.input(!stops.existsByRouteIdAndStopOrder(r.getId(), stop.getStopOrder()),
                        "Trùng thứ tự điểm dừng " + stop.getStopOrder());
                stops.saveAndFlush(stop);
                order = stop.getStopOrder() + 1;
            }
        }
        return toDetail(r);
    }

    @Transactional
    public RouteDetail update(Long id, RouteRequest req) {
        Route r = load(id);
        validate(req);
        if (req.routeCode() != null && !req.routeCode().isBlank()) {
            Guard.input(req.routeCode().equals(r.getRouteCode()), "Không đổi mã tuyến đã tạo");
        }
        Guard.input(req.alternativeOfId() == null || !req.alternativeOfId().equals(id), "Tuyến không thể thay thế chính nó");
        apply(r, req);
        return toDetail(r);
    }

    @Transactional
    public RouteDetail addStop(Long routeId, RouteStopRequest req) {
        Route r = load(routeId);
        ensureEditable(r);
        RouteStop stop = new RouteStop();
        stop.setRoute(r);
        applyStop(stop, req);
        int order = req.stopOrder() == null ? (int) stops.countByRouteId(routeId) + 1 : req.stopOrder();
        List<RouteStop> existing = stops.findByRouteIdOrderByStopOrderAsc(routeId);
        // Chèn vào giữa: dời các điểm phía sau xuống một bậc (từ cuối lên để không vi phạm unique)
        for (int i = existing.size() - 1; i >= 0; i--) {
            RouteStop s = existing.get(i);
            if (s.getStopOrder() >= order) {
                s.setStopOrder(s.getStopOrder() + 1);
                stops.saveAndFlush(s);
            }
        }
        stop.setStopOrder(order);
        stops.save(stop);
        return toDetail(r);
    }

    @Transactional
    public RouteDetail updateStop(Long routeId, Long stopId, RouteStopRequest req) {
        Route r = load(routeId);
        ensureEditable(r);
        RouteStop stop = stops.findById(stopId).filter(s -> s.getRoute().getId().equals(routeId))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy điểm dừng trên tuyến"));
        applyStop(stop, req);
        if (req.stopOrder() != null && !req.stopOrder().equals(stop.getStopOrder())) {
            Guard.input(!stops.existsByRouteIdAndStopOrder(routeId, req.stopOrder()),
                    "Thứ tự " + req.stopOrder() + " đã có điểm dừng khác");
            stop.setStopOrder(req.stopOrder());
        }
        return toDetail(r);
    }

    @Transactional
    public RouteDetail deleteStop(Long routeId, Long stopId) {
        Route r = load(routeId);
        ensureEditable(r);
        Guard.state(trips.findByRouteIdOrderByPlannedDepartureDesc(routeId).isEmpty(),
                "Tuyến đã được dùng cho chuyến; hãy tạo tuyến thay thế thay vì xóa điểm dừng");
        RouteStop stop = stops.findById(stopId).filter(s -> s.getRoute().getId().equals(routeId))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy điểm dừng trên tuyến"));
        stops.delete(stop);
        return toDetail(r);
    }

    public Route load(Long id) {
        return routes.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy tuyến"));
    }

    private void ensureEditable(Route r) {
        boolean running = trips.findByRouteIdOrderByPlannedDepartureDesc(r.getId()).stream()
                .map(TransportationTrip::getStatus).anyMatch(RUNNING::contains);
        Guard.state(!running, "Tuyến đang có chuyến chạy, không thể sửa điểm dừng");
    }

    private void validate(RouteRequest req) {
        Guard.input(Countries.isSupported(req.originCountry()), "Quốc gia đi không được hỗ trợ");
        Guard.input(Countries.isSupported(req.destinationCountry()), "Quốc gia đến không được hỗ trợ");
    }

    private void apply(Route r, RouteRequest req) {
        r.setName(req.name().trim());
        r.setOriginCountry(req.originCountry());
        r.setOriginLocation(req.originLocation());
        r.setDestinationCountry(req.destinationCountry());
        r.setDestinationLocation(req.destinationLocation());
        r.setTransportMode(req.transportMode());
        r.setEstimatedDistanceKm(req.estimatedDistanceKm());
        r.setEstimatedDurationHours(req.estimatedDurationHours());
        r.setStatus(req.status() == null ? RouteStatus.ACTIVE : req.status());
        r.setAlternativeOf(req.alternativeOfId() == null ? null : load(req.alternativeOfId()));
        r.setNotes(req.notes());
    }

    private static void applyStop(RouteStop stop, RouteStopRequest s) {
        if (s.country() != null) {
            Guard.input(Countries.isSupported(s.country()), "Quốc gia của điểm dừng không được hỗ trợ");
        }
        stop.setStopType(s.stopType());
        stop.setName(s.name().trim());
        stop.setAddress(s.address());
        stop.setCountry(s.country());
        stop.setLatitude(s.latitude());
        stop.setLongitude(s.longitude());
        stop.setPlannedArrivalOffsetMin(s.plannedArrivalOffsetMin());
        stop.setPlannedDurationMin(s.plannedDurationMin());
        stop.setInstructions(s.instructions());
    }

    public static RouteStopResponse toStop(RouteStop s) {
        return new RouteStopResponse(s.getId(), s.getStopOrder(), s.getStopType(), s.getName(), s.getAddress(),
                s.getCountry(), s.getLatitude(), s.getLongitude(), s.getPlannedArrivalOffsetMin(),
                s.getPlannedDurationMin(), s.getInstructions());
    }

    private RouteSummary toSummary(Route r, int stopCount) {
        return new RouteSummary(r.getId(), r.getRouteCode(), r.getName(), r.getOriginCountry(), r.getOriginLocation(),
                r.getDestinationCountry(), r.getDestinationLocation(), r.getTransportMode(),
                r.getEstimatedDistanceKm(), r.getEstimatedDurationHours(), r.getStatus(),
                r.getAlternativeOf() == null ? null : r.getAlternativeOf().getId(), stopCount, r.getUpdatedAt());
    }

    public RouteDetail toDetail(Route r) {
        List<RouteStopResponse> stopList = stops.findByRouteIdOrderByStopOrderAsc(r.getId()).stream()
                .map(RouteService::toStop).toList();
        List<RouteSummary> alternatives = routes.findByAlternativeOfId(r.getId()).stream()
                .map(a -> toSummary(a, (int) stops.countByRouteId(a.getId()))).toList();
        long tripCount = trips.findByRouteIdOrderByPlannedDepartureDesc(r.getId()).size();
        return new RouteDetail(r.getId(), r.getRouteCode(), r.getName(), r.getOriginCountry(), r.getOriginLocation(),
                r.getDestinationCountry(), r.getDestinationLocation(), r.getTransportMode(),
                r.getEstimatedDistanceKm(), r.getEstimatedDurationHours(), r.getStatus(),
                r.getAlternativeOf() == null ? null : r.getAlternativeOf().getId(),
                r.getAlternativeOf() == null ? null : r.getAlternativeOf().getName(), r.getNotes(), stopList,
                alternatives, tripCount, r.getUpdatedAt());
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /**
     * Xóa tuyến: chưa từng dùng cho chuyến, không là tuyến thay thế của tuyến khác và không nằm trong quyết định đổi tuyến
     * thì xóa hẳn cùng các điểm dừng; ngược lại chuyển "Ngừng dùng" để giữ lịch sử chuyến.
     * Trả về true nếu đã xóa hẳn.
     */
    @Transactional
    public boolean delete(Long id) {
        Route r = load(id);
        Guard.state(trips.findByRouteIdOrderByPlannedDepartureDesc(id).stream().noneMatch(t -> RUNNING.contains(t.getStatus())
                        || t.getStatus() == TripStatus.SCHEDULED || t.getStatus() == TripStatus.PREPARING
                        || t.getStatus() == TripStatus.READY_FOR_DEPARTURE),
                "Tuyến đang được dùng cho chuyến chưa kết thúc");
        boolean used = !trips.findByRouteIdOrderByPlannedDepartureDesc(id).isEmpty()
                || !routes.findByAlternativeOfId(id).isEmpty() || routes.usedByIncidentAction(id);
        if (used) {
            r.setStatus(RouteStatus.INACTIVE);
            return false;
        }
        stops.deleteAll(stops.findByRouteIdOrderByStopOrderAsc(id));
        routes.delete(r);
        return true;
    }
}
