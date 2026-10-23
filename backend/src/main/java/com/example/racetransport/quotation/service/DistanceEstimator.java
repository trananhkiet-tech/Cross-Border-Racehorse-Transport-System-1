package com.example.racetransport.quotation.service;

import com.example.racetransport.route.entity.RouteStatus;
import com.example.racetransport.route.repository.RouteRepository;
import com.example.racetransport.transportationrequest.entity.TransportMode;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Ước tính quãng đường cho Estimated Cost (không dùng dịch vụ bản đồ trả phí):
 * 1) nếu có tuyến ACTIVE cùng cặp quốc gia + phương thức → dùng quãng đường của tuyến;
 * 2) nếu không, dùng bảng khoảng cách DEMO theo cặp quốc gia;
 * 3) mặc định 800 km. Đây chỉ là số ước tính, Manager xác nhận lại khi lập báo giá cuối.
 */
@Component
public class DistanceEstimator {

    private static final Map<String, Integer> DEMO_KM = Map.ofEntries(
            Map.entry("VN-VN", 300), Map.entry("VN-KH", 250), Map.entry("VN-TH", 950), Map.entry("VN-LA", 900),
            Map.entry("VN-MY", 1900), Map.entry("VN-SG", 2100), Map.entry("TH-MY", 1200), Map.entry("TH-SG", 1450),
            Map.entry("MY-SG", 350), Map.entry("TH-KH", 650), Map.entry("TH-TH", 400), Map.entry("SG-SG", 50));

    private final RouteRepository routes;

    public DistanceEstimator(RouteRepository routes) {
        this.routes = routes;
    }

    public BigDecimal estimateKm(TransportationRequest r) {
        TransportMode mode = r.getTransportMode();
        var route = routes.findFirstByOriginCountryAndDestinationCountryAndTransportModeAndStatusOrderByIdAsc(
                r.getOriginCountry(), r.getDestinationCountry(), mode, RouteStatus.ACTIVE);
        if (route.isPresent() && route.get().getEstimatedDistanceKm() != null) {
            return route.get().getEstimatedDistanceKm();
        }
        Integer km = DEMO_KM.get(r.getOriginCountry() + "-" + r.getDestinationCountry());
        if (km == null) {
            km = DEMO_KM.get(r.getDestinationCountry() + "-" + r.getOriginCountry());
        }
        return BigDecimal.valueOf(km == null ? 800 : km);
    }

    /** Số ngày hành trình ước tính: 500 km/ngày, tối thiểu 1 ngày; hàng không tính 1 ngày + 1 ngày bộ. */
    public int estimateDays(TransportationRequest r, BigDecimal km) {
        if (r.getTransportMode() == TransportMode.AIR || r.getTransportMode() == TransportMode.ROAD_AIR) {
            return 2;
        }
        return Math.max(1, (int) Math.ceil(km.doubleValue() / 500.0));
    }
}
