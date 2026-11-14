package com.example.racetransport.route;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.racetransport.common.exception.ConflictException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.route.dto.RouteDtos.RouteDetail;
import com.example.racetransport.route.dto.RouteDtos.RouteRequest;
import com.example.racetransport.route.entity.Route;
import com.example.racetransport.route.entity.RouteStatus;
import com.example.racetransport.route.entity.StopType;
import com.example.racetransport.route.repository.RouteRepository;
import com.example.racetransport.route.repository.RouteStopRepository;
import com.example.racetransport.route.service.RouteService;
import com.example.racetransport.transportationrequest.entity.TransportMode;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests cho RouteService – quản lý tuyến đường vận chuyển ngựa đua quốc tế.
 * Kiểm tra nghiệp vụ: tạo tuyến, kích hoạt, vô hiệu hóa, truy vấn tuyến active.
 */
class RouteServiceTest {

    private RouteRepository routeRepo;
    private RouteStopRepository stopRepo;
    private TransportationTripRepository tripRepo;
    private CodeGenerator codeGen;
    private RouteService service;

    @BeforeEach
    void setUp() {
        routeRepo = mock(RouteRepository.class);
        stopRepo  = mock(RouteStopRepository.class);
        tripRepo  = mock(TransportationTripRepository.class);
        codeGen   = mock(CodeGenerator.class);
        service   = new RouteService(routeRepo, stopRepo, tripRepo, codeGen);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Route makeRoute(Long id, RouteStatus status) {
        Route r = new Route();
        r.setId(id);
        r.setRouteCode("ROUTE-" + id);
        r.setName("Tuyến HCM → Singapore " + id);
        r.setOriginCountry("VN");
        r.setOriginLocation("TP. Hồ Chí Minh");
        r.setDestinationCountry("SG");
        r.setDestinationLocation("Singapore");
        r.setTransportMode(TransportMode.AIR);
        r.setEstimatedDistanceKm(new BigDecimal("1200.0"));
        r.setEstimatedDurationHours(new BigDecimal("24.0"));
        r.setStatus(status);
        r.setVersion(0L);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    // ─── get() ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("get – tìm thấy tuyến đường theo id")
    void get_found() {
        Route r = makeRoute(1L, RouteStatus.ACTIVE);
        when(routeRepo.findById(1L)).thenReturn(Optional.of(r));
        when(stopRepo.findByRouteIdOrderByStopOrder(1L)).thenReturn(Collections.emptyList());

        RouteDetail detail = service.get(1L);

        assertEquals("ROUTE-1", detail.routeCode());
        assertEquals(RouteStatus.ACTIVE, detail.status());
    }

    @Test
    @DisplayName("get – không tìm thấy tuyến → NotFoundException")
    void get_notFound() {
        when(routeRepo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.get(999L));
    }

    // ─── activate() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("activate – tuyến DRAFT có điểm dừng → chuyển sang ACTIVE")
    void activate_draftWithStops_ok() {
        Route r = makeRoute(2L, RouteStatus.DRAFT);
        when(routeRepo.findById(2L)).thenReturn(Optional.of(r));
        when(stopRepo.countByRouteId(2L)).thenReturn(2L);
        when(routeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stopRepo.findByRouteIdOrderByStopOrder(2L)).thenReturn(Collections.emptyList());

        RouteDetail detail = service.activate(2L);

        assertEquals(RouteStatus.ACTIVE, detail.status());
    }

    @Test
    @DisplayName("activate – tuyến DRAFT không có điểm dừng → ConflictException")
    void activate_noStops_conflict() {
        Route r = makeRoute(3L, RouteStatus.DRAFT);
        when(routeRepo.findById(3L)).thenReturn(Optional.of(r));
        when(stopRepo.countByRouteId(3L)).thenReturn(0L);

        assertThrows(ConflictException.class, () -> service.activate(3L));
    }

    // ─── deactivate() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivate – tuyến ACTIVE không có chuyến đang chạy → chuyển INACTIVE")
    void deactivate_noRunningTrips_ok() {
        Route r = makeRoute(4L, RouteStatus.ACTIVE);
        when(routeRepo.findById(4L)).thenReturn(Optional.of(r));
        when(tripRepo.countActiveByRouteId(any(), eq(4L))).thenReturn(0L);
        when(routeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stopRepo.findByRouteIdOrderByStopOrder(4L)).thenReturn(Collections.emptyList());

        RouteDetail detail = service.deactivate(4L);

        assertEquals(RouteStatus.INACTIVE, detail.status());
    }

    @Test
    @DisplayName("deactivate – tuyến ACTIVE đang có chuyến → ConflictException")
    void deactivate_hasRunningTrips_conflict() {
        Route r = makeRoute(5L, RouteStatus.ACTIVE);
        when(routeRepo.findById(5L)).thenReturn(Optional.of(r));
        when(tripRepo.countActiveByRouteId(any(), eq(5L))).thenReturn(2L);

        assertThrows(ConflictException.class, () -> service.deactivate(5L));
    }

    // ─── list() ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("list – filter theo ACTIVE → chỉ trả tuyến active")
    void list_filterActive() {
        Route r = makeRoute(6L, RouteStatus.ACTIVE);
        when(routeRepo.search(any(), any(), any())).thenReturn(List.of(r));

        var result = service.list(List.of(RouteStatus.ACTIVE), null, null);

        assertEquals(1, result.size());
        assertEquals("ROUTE-6", result.get(0).routeCode());
    }

    @Test
    @DisplayName("list – không có tuyến nào → trả danh sách rỗng")
    void list_empty() {
        when(routeRepo.search(any(), any(), any())).thenReturn(Collections.emptyList());
        assertTrue(service.list(null, null, null).isEmpty());
    }
}
