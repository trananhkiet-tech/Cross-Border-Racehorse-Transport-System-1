package com.example.racetransport.config.seed;

import com.example.racetransport.config.AppProperties;
import com.example.racetransport.route.dto.RouteDtos.RouteDetail;
import com.example.racetransport.route.dto.RouteDtos.RouteRequest;
import com.example.racetransport.route.dto.RouteDtos.RouteStopRequest;
import com.example.racetransport.route.dto.RouteDtos.RouteStopResponse;
import com.example.racetransport.route.dto.RouteDtos.RouteSummary;
import com.example.racetransport.route.entity.RouteStatus;
import com.example.racetransport.route.entity.StopType;
import com.example.racetransport.route.service.RouteService;
import com.example.racetransport.transportationrequest.entity.TransportMode;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.EmployeeProfile;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.entity.StaffRole;
import com.example.racetransport.user.repository.AppUserRepository;
import com.example.racetransport.user.repository.EmployeeProfileRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Bổ sung dữ liệu DEMO cho profile dev: tuyến đường đi/về cho TẤT CẢ quốc gia trong danh mục
 * ({@link com.example.racetransport.common.util.Countries}) và thêm tài xế / người áp tải.
 * Khác {@link DemoDataSeeder}: chạy được cả khi DB đã có dữ liệu, <b>idempotent</b> (kiểm tra mã tuyến / email
 * đã tồn tại thì bỏ qua), không xóa hay sửa dữ liệu cũ. Khoảng cách/thời gian chỉ là ƯỚC TÍNH cho demo.
 */
@Component
@Profile("dev")
@Order(20)
public class DemoDataExtender implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataExtender.class);

    private static final double[] SAO_MAI = {10.9733, 106.4931};
    private static final double[] TSN = {10.8188, 106.6520};

    private final AppProperties props;
    private final TransactionTemplate tx;
    private final RouteService routeService;
    private final AppUserRepository users;
    private final EmployeeProfileRepository employees;
    private final PasswordEncoder encoder;

    public DemoDataExtender(AppProperties props, TransactionTemplate tx, RouteService routeService,
                            AppUserRepository users, EmployeeProfileRepository employees, PasswordEncoder encoder) {
        this.props = props;
        this.tx = tx;
        this.routeService = routeService;
        this.users = users;
        this.employees = employees;
        this.encoder = encoder;
    }

    /** Điểm đến đường hàng không (kết hợp đường bộ ra sân bay). */
    private record AirDest(String cc, String codeSuffix, String country, String city, String airport, double[] airportLl,
                           String quarantine, double[] quarantineLl, String venue, double[] venueLl, int flightMin,
                           boolean createOutbound) { }

    private static final List<AirDest> AIR = List.of(
            new AirDest("SG", "SIN", "Singapore", "Kranji, Singapore", "Sân bay Changi – Animal Holding", new double[]{1.3644, 103.9915},
                    "Trạm cách ly ngựa Kranji", new double[]{1.4250, 103.7560}, "Singapore Turf Club, Kranji", new double[]{1.4240, 103.7590}, 120, false),
            new AirDest("HK", "HKG", "Hồng Kông", "Sha Tin, Hồng Kông", "Sân bay quốc tế Hồng Kông – kho động vật", new double[]{22.3080, 113.9185},
                    "Khu cách ly ngựa Sha Tin", new double[]{22.4015, 114.2040}, "Trường đua Sha Tin", new double[]{22.4010, 114.2030}, 170, true),
            new AirDest("JP", "NRT", "Nhật Bản", "Chiba, Nhật Bản", "Sân bay Narita – kho động vật", new double[]{35.7720, 140.3929},
                    "Trạm kiểm dịch ngựa Negishi (Yokohama)", new double[]{35.4000, 139.6500}, "Trường đua Nakayama", new double[]{35.7240, 139.9580}, 360, true),
            new AirDest("KR", "ICN", "Hàn Quốc", "Gwacheon, Hàn Quốc", "Sân bay Incheon – kho động vật", new double[]{37.4602, 126.4407},
                    "Trạm kiểm dịch Incheon", new double[]{37.4700, 126.4500}, "Seoul Race Park, Gwacheon", new double[]{37.4430, 127.0050}, 330, true),
            new AirDest("CN", "CAN", "Trung Quốc", "Quảng Châu, Trung Quốc", "Sân bay Bạch Vân Quảng Châu – kho động vật", new double[]{23.3924, 113.2988},
                    "Khu kiểm dịch động vật Quảng Châu", new double[]{23.4300, 113.3300}, "Trung tâm ngựa Quảng Châu (Conghua)", new double[]{23.5500, 113.5800}, 150, true),
            new AirDest("AU", "MEL", "Úc", "Melbourne, Úc", "Sân bay Tullamarine – kho động vật", new double[]{-37.6690, 144.8410},
                    "Trạm cách ly ngựa Spotswood", new double[]{-37.8300, 144.8800}, "Trường đua Flemington", new double[]{-37.7870, 144.9120}, 560, true),
            new AirDest("AE", "DXB", "UAE", "Dubai, UAE", "Sân bay quốc tế Dubai – kho động vật", new double[]{25.2532, 55.3657},
                    "Trạm cách ly ngựa Dubai", new double[]{25.1700, 55.3100}, "Trường đua Meydan", new double[]{25.1620, 55.3030}, 460, true),
            new AirDest("GB", "LHR", "Vương quốc Anh", "Newmarket, Anh", "Sân bay Heathrow – Animal Reception Centre", new double[]{51.4700, -0.4543},
                    "Trạm kiểm dịch Heathrow", new double[]{51.4800, -0.4700}, "Trường đua Newmarket", new double[]{52.2400, 0.4000}, 780, true),
            new AirDest("FR", "CDG", "Pháp", "Chantilly, Pháp", "Sân bay Charles de Gaulle – kho động vật", new double[]{49.0097, 2.5479},
                    "Trạm kiểm dịch Roissy", new double[]{49.0050, 2.5700}, "Trường đua Chantilly", new double[]{49.1930, 2.4700}, 760, true),
            new AirDest("IE", "DUB", "Ireland", "Curragh, Ireland", "Sân bay Dublin – kho động vật", new double[]{53.4264, -6.2499},
                    "Trạm kiểm dịch Dublin", new double[]{53.4000, -6.3000}, "Trường đua The Curragh", new double[]{53.1500, -6.8300}, 800, true),
            new AirDest("US", "JFK", "Hoa Kỳ", "Elmont (New York), Hoa Kỳ", "Sân bay JFK – Animal Import Center", new double[]{40.6413, -73.7781},
                    "Trạm kiểm dịch JFK", new double[]{40.6500, -73.7800}, "Belmont Park", new double[]{40.7150, -73.7260}, 1080, true));

    @Override
    public void run(ApplicationArguments args) {
        if (!props.seed().demoData()) {
            return;
        }
        step("Tuyến đường cho mọi quốc gia", this::seedRoutes);
        step("Tài xế / người áp tải bổ sung", this::seedStaff);
    }

    private void step(String name, Runnable body) {
        try {
            tx.executeWithoutResult(status -> body.run());
            log.info("Demo extend OK: {}", name);
        } catch (RuntimeException e) {
            log.error("Demo extend LỖI ở bước '{}': {}", name, e.getMessage(), e);
        }
    }

    // ================================================================== routes
    private void seedRoutes() {
        // 1) Đường bộ: Lào (mới); Thái Lan, Campuchia, Malaysia đã có tuyến đi trong seed gốc.
        createIfAbsent(laosOutbound());
        // 2) Đường hàng không cho 11 quốc gia còn lại (Singapore đã có tuyến đi).
        for (AirDest d : AIR) {
            if (d.createOutbound()) {
                createIfAbsent(airOutbound(d));
            }
            createIfAbsent(airInbound(d));
        }
        // 3) Chiều về cho các tuyến đường bộ: đảo ngược tuyến đi đã có.
        reverseOf("RT-HCM-BKK", "RT-BKK-HCM", "Bangkok → TP.HCM (qua Poipet – Mộc Bài)");
        reverseOf("RT-HCM-PNH", "RT-PNH-HCM", "Phnom Penh → TP.HCM");
        reverseOf("RT-HCM-KUL", "RT-KUL-HCM", "Kuala Lumpur → TP.HCM (đường bộ)");
        reverseOf("RT-HCM-VTE", "RT-VTE-HCM", "Vientiane → TP.HCM (qua Lao Bảo)");
    }

    private RouteRequest laosOutbound() {
        List<RouteStopRequest> st = List.of(
                stop(StopType.PICKUP, "Trại Sao Mai, Củ Chi", "VN", 10.9733, 106.4931, 0, 45, "Kiểm tra trước vận chuyển, xếp ngựa lên xe"),
                stop(StopType.STABLE, "Chuồng trung chuyển Đà Nẵng", "VN", 16.0544, 108.2022, 1080, 480, "Nghỉ qua đêm, kiểm tra sức khỏe ngựa"),
                stop(StopType.BORDER, "Cửa khẩu Lao Bảo", "VN", 16.6190, 106.5990, 1560, 90, "Xuất trình hồ sơ xuất khẩu, kiểm dịch"),
                stop(StopType.CUSTOMS, "Hải quan Densavan", "LA", 16.6330, 106.5300, 1660, 60, "Thủ tục nhập cảnh Lào"),
                stop(StopType.REST_STOP, "Điểm nghỉ Savannakhet", "LA", 16.5570, 104.7520, 1900, 90, null),
                stop(StopType.DESTINATION, "Trung tâm ngựa Vientiane", "LA", 17.9757, 102.6331, 2400, 60, "Bàn giao cho đại diện khách hàng"));
        return request("RT-HCM-VTE", "TP.HCM → Vientiane (qua Lao Bảo – Densavan)", "VN", "TP. Hồ Chí Minh", "LA", "Vientiane, Lào",
                TransportMode.ROAD, st);
    }

    private RouteRequest airOutbound(AirDest d) {
        int arrive = 300 + d.flightMin();
        List<RouteStopRequest> st = List.of(
                stop(StopType.PICKUP, "Trại Sao Mai, Củ Chi", "VN", SAO_MAI[0], SAO_MAI[1], 0, 60, "Kiểm tra trước vận chuyển, xếp ngựa lên xe"),
                stop(StopType.AIRPORT, "Sân bay Tân Sơn Nhất – kho hàng động vật", "VN", TSN[0], TSN[1], 120, 180, "Đóng air stall, kiểm dịch xuất"),
                stop(StopType.AIRPORT, d.airport(), d.cc(), d.airportLl()[0], d.airportLl()[1], arrive, 120, "Nhận ngựa, thủ tục hải quan nhập"),
                stop(StopType.QUARANTINE, d.quarantine(), d.cc(), d.quarantineLl()[0], d.quarantineLl()[1], arrive + 210, 0,
                        "Cách ly theo yêu cầu nước nhập (thời gian cách ly tùy nước – demo)"),
                stop(StopType.DESTINATION, d.venue(), d.cc(), d.venueLl()[0], d.venueLl()[1], arrive + 330, 60, "Bàn giao cho đại diện khách hàng"));
        return request("RT-HCM-" + d.codeSuffix(), "TP.HCM → " + d.country() + " (hàng không)", "VN", "TP. Hồ Chí Minh",
                d.cc(), d.city(), TransportMode.ROAD_AIR, st);
    }

    private RouteRequest airInbound(AirDest d) {
        int arrive = 330 + d.flightMin();
        List<RouteStopRequest> st = List.of(
                stop(StopType.PICKUP, d.venue(), d.cc(), d.venueLl()[0], d.venueLl()[1], 0, 60, "Kiểm tra trước vận chuyển, xếp ngựa lên xe"),
                stop(StopType.AIRPORT, d.airport(), d.cc(), d.airportLl()[0], d.airportLl()[1], 150, 180, "Đóng air stall, kiểm dịch xuất"),
                stop(StopType.AIRPORT, "Sân bay Tân Sơn Nhất – kho hàng động vật", "VN", TSN[0], TSN[1], arrive, 120, "Nhận ngựa, thủ tục hải quan nhập"),
                stop(StopType.QUARANTINE, "Trạm kiểm dịch động vật Tân Sơn Nhất", "VN", 10.8200, 106.6600, arrive + 150, 0, "Kiểm dịch nhập khẩu (demo)"),
                stop(StopType.DESTINATION, "Trại Sao Mai, Củ Chi", "VN", SAO_MAI[0], SAO_MAI[1], arrive + 270, 60, "Bàn giao cho khách hàng"));
        return request("RT-" + d.codeSuffix() + "-HCM", d.country() + " → TP.HCM (hàng không)", d.cc(), d.venue(),
                "VN", "TP. Hồ Chí Minh", TransportMode.ROAD_AIR, st);
    }

    /** Tạo tuyến chiều về bằng cách đảo ngược tuyến đi đã có trong DB (nếu tuyến đi tồn tại). */
    private void reverseOf(String outboundCode, String newCode, String newName) {
        RouteSummary s = find(outboundCode);
        if (s == null) {
            log.warn("Demo extend: không thấy tuyến {} để tạo chiều về {}", outboundCode, newCode);
            return;
        }
        RouteDetail r = routeService.get(s.id());
        List<RouteStopResponse> src = new ArrayList<>(r.stops());
        src.sort((a, b) -> Integer.compare(a.stopOrder(), b.stopOrder()));
        int total = src.get(src.size() - 1).plannedArrivalOffsetMin() == null ? 0 : src.get(src.size() - 1).plannedArrivalOffsetMin();
        List<RouteStopRequest> rev = new ArrayList<>();
        List<RouteStopResponse> back = new ArrayList<>(src);
        Collections.reverse(back);
        for (int i = 0; i < back.size(); i++) {
            RouteStopResponse o = back.get(i);
            StopType type = i == 0 ? StopType.PICKUP : i == back.size() - 1 ? StopType.DESTINATION : o.stopType();
            int off = total - (o.plannedArrivalOffsetMin() == null ? 0 : o.plannedArrivalOffsetMin());
            rev.add(new RouteStopRequest(null, type, o.name(), o.address(), o.country(), o.latitude(), o.longitude(), off,
                    o.plannedDurationMin(), o.instructions()));
        }
        createIfAbsent(new RouteRequest(newCode, newName, r.destinationCountry(), r.destinationLocation(), r.originCountry(),
                r.originLocation(), r.transportMode(), r.estimatedDistanceKm(), r.estimatedDurationHours(), RouteStatus.ACTIVE,
                null, "Chiều về của tuyến " + outboundCode + " (dữ liệu demo)", rev));
    }

    private RouteSummary find(String code) {
        return routeService.list(null, null, null, code).stream()
                .filter(x -> code.equalsIgnoreCase(x.routeCode())).findFirst().orElse(null);
    }

    private void createIfAbsent(RouteRequest req) {
        if (find(req.routeCode()) != null) {
            return;
        }
        routeService.create(req);
        log.info("Demo extend: đã thêm tuyến {} – {}", req.routeCode(), req.name());
    }

    private RouteRequest request(String code, String name, String oc, String oLoc, String dc, String dLoc, TransportMode mode,
                                 List<RouteStopRequest> st) {
        double km = 0;
        for (int i = 1; i < st.size(); i++) {
            RouteStopRequest a = st.get(i - 1);
            RouteStopRequest b = st.get(i);
            boolean air = a.stopType() == StopType.AIRPORT && b.stopType() == StopType.AIRPORT;
            km += haversine(a.latitude().doubleValue(), a.longitude().doubleValue(), b.latitude().doubleValue(), b.longitude().doubleValue())
                    * (air ? 1.0 : 1.25);
        }
        RouteStopRequest last = st.get(st.size() - 1);
        double hours = (last.plannedArrivalOffsetMin() + last.plannedDurationMin()) / 60.0;
        return new RouteRequest(code, name, oc, oLoc, dc, dLoc, mode,
                BigDecimal.valueOf(Math.max(1, Math.round(km))), BigDecimal.valueOf(hours).setScale(1, RoundingMode.HALF_UP),
                RouteStatus.ACTIVE, null, "Dữ liệu demo – khoảng cách/thời gian là ước tính, không phải lịch bay hay quy định thực tế.", st);
    }

    private static RouteStopRequest stop(StopType type, String name, String country, double lat, double lng, int offsetMin,
                                         int durationMin, String instructions) {
        return new RouteStopRequest(null, type, name, null, country, BigDecimal.valueOf(lat), BigDecimal.valueOf(lng), offsetMin,
                durationMin, instructions);
    }

    private static double haversine(double la1, double lo1, double la2, double lo2) {
        double dLa = Math.toRadians(la2 - la1);
        double dLo = Math.toRadians(lo2 - lo1);
        double a = Math.sin(dLa / 2) * Math.sin(dLa / 2)
                + Math.cos(Math.toRadians(la1)) * Math.cos(Math.toRadians(la2)) * Math.sin(dLo / 2) * Math.sin(dLo / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ================================================================== staff
    private void seedStaff() {
        // email, họ tên, mã NV, vai trò, số GPLX (null nếu không có), nơi đóng quân
        staff("driver3@gmail.com", "Phạm Đức Thắng", "DR-003", StaffRole.DRIVER, "79C-446688", "TP. Hồ Chí Minh");
        staff("driver4@gmail.com", "Nguyễn Văn Hậu", "DR-004", StaffRole.DRIVER, "79C-557799", "TP. Hồ Chí Minh");
        staff("driver5@gmail.com", "Bùi Quang Huy", "DR-005", StaffRole.DRIVER, "43C-228811", "Đà Nẵng");
        staff("driver6@gmail.com", "Đỗ Thanh Tùng", "DR-006", StaffRole.DRIVER, "92C-339922", "Cần Thơ");
        staff("driver7@gmail.com", "Vũ Minh Hiếu", "DR-007", StaffRole.DRIVER, "29C-440033", "Hà Nội");
        staff("driver8@gmail.com", "Somchai Prasert", "DR-008", StaffRole.DRIVER, "TH-BKK-118226", "Bangkok, Thái Lan");
        staff("driver9@gmail.com", "Lim Wei Jie", "DR-009", StaffRole.DRIVER, "SG-KRJ-224466", "Singapore");
        staff("driver10@gmail.com", "Ahmad Faizal", "DR-010", StaffRole.DRIVER, "MY-KUL-335577", "Kuala Lumpur, Malaysia");
        staff("escort4@gmail.com", "Lê Thị Mai", "ES-004", StaffRole.ESCORT, null, "TP. Hồ Chí Minh");
        staff("escort5@gmail.com", "Trần Quốc Khánh", "ES-005", StaffRole.ESCORT, null, "TP. Hồ Chí Minh");
        staff("escort6@gmail.com", "Ngô Bảo Châu", "ES-006", StaffRole.ESCORT, null, "Đà Nẵng");
        staff("escort7@gmail.com", "Phan Thị Thu", "ES-007", StaffRole.ESCORT, null, "Hà Nội");
        staff("escort8@gmail.com", "Kenji Yamamoto", "ES-008", StaffRole.ESCORT, null, "Tokyo, Nhật Bản");
        staff("escort9@gmail.com", "Park Ji-ho", "ES-009", StaffRole.ESCORT, null, "Seoul, Hàn Quốc");
        staff("escort10@gmail.com", "Emily Carter", "ES-010", StaffRole.ESCORT, null, "Melbourne, Úc");
        staff("escort11@gmail.com", "Khalid Al-Mansoori", "ES-011", StaffRole.ESCORT, null, "Dubai, UAE");
        staff("escort12@gmail.com", "Oliver Hughes", "ES-012", StaffRole.ESCORT, null, "Newmarket, Anh");
        staff("escort13@gmail.com", "Camille Laurent", "ES-013", StaffRole.ESCORT, null, "Chantilly, Pháp");
        staff("escort14@gmail.com", "Sean O'Brien", "ES-014", StaffRole.ESCORT, null, "Curragh, Ireland");
        staff("escort15@gmail.com", "Michael Thompson", "ES-015", StaffRole.ESCORT, null, "New York, Hoa Kỳ");
        staff("leader2@gmail.com", "Hoàng Gia Bảo", "TL-002", StaffRole.TEAM_LEADER, "79C-661100", "TP. Hồ Chí Minh");
        staff("leader3@gmail.com", "Trương Mỹ Linh", "TL-003", StaffRole.TEAM_LEADER, "43C-772211", "Đà Nẵng");
        staff("leader4@gmail.com", "Chen Wei Ming", "TL-004", StaffRole.TEAM_LEADER, "HK-STN-883322", "Hồng Kông");
    }

    private void staff(String email, String name, String code, StaffRole staffRole, String license, String base) {
        if (users.existsByEmailIgnoreCase(email) || employees.existsByEmployeeCode(code)) {
            return;
        }
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setFullName(name);
        u.setPhone("0911 000 " + code.substring(code.length() - 3));
        u.setRole(Role.DRIVER_ESCORT);
        u.setEnabled(true);
        u.setPasswordHash(encoder.encode(DemoDataSeeder.DEMO_PASSWORD));
        users.save(u);
        EmployeeProfile p = new EmployeeProfile();
        p.setUser(u);
        p.setEmployeeCode(code);
        p.setDepartment("Vận hành");
        p.setStaffRole(staffRole);
        p.setLicenseNumber(license);
        p.setLicenseExpiry(license == null ? null : LocalDate.of(2030, 6, 30));
        p.setAirEscortCertified(staffRole == StaffRole.ESCORT || staffRole == StaffRole.TEAM_LEADER);
        p.setEmergencyContact("Phòng điều hành IRTMS – 028 3838 0000");
        p.setBaseLocation(base);
        employees.save(p);
        log.info("Demo extend: đã thêm nhân sự {} – {}", code, name);
    }
}
