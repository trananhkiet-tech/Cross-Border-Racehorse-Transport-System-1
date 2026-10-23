package com.example.racetransport.config.seed;

import com.example.racetransport.claim.dto.ClaimDtos.CreateClaimRequest;
import com.example.racetransport.claim.dto.ClaimDtos.ReviewClaimRequest;
import com.example.racetransport.claim.entity.ClaimRecommendation;
import com.example.racetransport.claim.entity.ClaimType;
import com.example.racetransport.claim.entity.EvidenceType;
import com.example.racetransport.claim.service.ClaimService;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.compliance.dto.ComplianceDtos.ChecklistItemResponse;
import com.example.racetransport.compliance.entity.ChecklistItemStatus;
import com.example.racetransport.compliance.service.ComplianceService;
import com.example.racetransport.config.AppClock;
import com.example.racetransport.config.AppProperties;
import com.example.racetransport.document.dto.DocumentDtos.DocumentResponse;
import com.example.racetransport.document.dto.DocumentDtos.ReviewRequest;
import com.example.racetransport.document.entity.AttachmentRefType;
import com.example.racetransport.document.entity.DocumentReviewStatus;
import com.example.racetransport.document.entity.DocumentType;
import com.example.racetransport.document.service.AttachmentService;
import com.example.racetransport.document.service.DocumentService;
import com.example.racetransport.fleet.dto.VehicleDtos.VehicleRequest;
import com.example.racetransport.fleet.dto.VehicleDtos.VehicleResponse;
import com.example.racetransport.fleet.entity.VehicleStatus;
import com.example.racetransport.fleet.entity.VehicleType;
import com.example.racetransport.fleet.service.VehicleService;
import com.example.racetransport.handover.dto.HandoverDtos.ConfirmHandoverRequest;
import com.example.racetransport.handover.dto.HandoverDtos.HandoverRequest;
import com.example.racetransport.handover.dto.HandoverDtos.HandoverResponse;
import com.example.racetransport.handover.service.HandoverService;
import com.example.racetransport.health.dto.HealthDtos.ArrivalCheckRequest;
import com.example.racetransport.health.dto.HealthDtos.ObservationRequest;
import com.example.racetransport.health.dto.HealthDtos.ObservationResponse;
import com.example.racetransport.health.dto.HealthDtos.PreTransportCheckRequest;
import com.example.racetransport.health.dto.HealthDtos.VetConsultationRequest;
import com.example.racetransport.health.entity.HorseBehavior;
import com.example.racetransport.health.entity.HorseCondition;
import com.example.racetransport.health.entity.IntakeLevel;
import com.example.racetransport.health.entity.MovementCondition;
import com.example.racetransport.health.entity.StressLevel;
import com.example.racetransport.health.service.HealthService;
import com.example.racetransport.horse.dto.HorseDtos.HorseRequest;
import com.example.racetransport.horse.dto.HorseDtos.HorseResponse;
import com.example.racetransport.horse.dto.HorseDtos.StallTypeResponse;
import com.example.racetransport.horse.entity.HorseSex;
import com.example.racetransport.horse.service.HorseService;
import com.example.racetransport.incident.dto.IncidentDtos.IncidentActionRequest;
import com.example.racetransport.incident.dto.IncidentDtos.IncidentDetail;
import com.example.racetransport.incident.dto.IncidentDtos.ReportIncidentRequest;
import com.example.racetransport.incident.entity.IncidentActionType;
import com.example.racetransport.incident.entity.IncidentSeverity;
import com.example.racetransport.incident.entity.IncidentStatus;
import com.example.racetransport.incident.entity.IncidentType;
import com.example.racetransport.incident.entity.OperationalDecision;
import com.example.racetransport.incident.service.IncidentService;
import com.example.racetransport.payment.dto.PaymentDtos.SimulatePaymentRequest;
import com.example.racetransport.payment.entity.PaymentMethod;
import com.example.racetransport.payment.service.InvoiceService;
import com.example.racetransport.payment.service.PaymentService;
import com.example.racetransport.quotation.dto.QuoteDtos.QuoteItemInput;
import com.example.racetransport.quotation.dto.QuoteDtos.QuoteItemResponse;
import com.example.racetransport.quotation.dto.QuoteDtos.QuoteResponse;
import com.example.racetransport.quotation.dto.QuoteDtos.QuoteUpdateRequest;
import com.example.racetransport.quotation.dto.QuoteDtos.RespondRequest;
import com.example.racetransport.quotation.entity.CostCategory;
import com.example.racetransport.quotation.service.QuoteService;
import com.example.racetransport.route.dto.RouteDtos.RouteDetail;
import com.example.racetransport.route.dto.RouteDtos.RouteRequest;
import com.example.racetransport.route.dto.RouteDtos.RouteStopRequest;
import com.example.racetransport.route.dto.RouteDtos.RouteStopResponse;
import com.example.racetransport.route.entity.RouteStatus;
import com.example.racetransport.route.entity.StopType;
import com.example.racetransport.route.service.RouteService;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.tracking.entity.TrackingEventType;
import com.example.racetransport.transportationrequest.dto.RequestDtos.DecisionRequest;
import com.example.racetransport.transportationrequest.dto.RequestDtos.RequestDetail;
import com.example.racetransport.transportationrequest.dto.RequestDtos.RequestHorseInput;
import com.example.racetransport.transportationrequest.dto.RequestDtos.RequestUpsert;
import com.example.racetransport.transportationrequest.entity.MovementType;
import com.example.racetransport.transportationrequest.entity.TransportMode;
import com.example.racetransport.transportationrequest.entity.TransportPurpose;
import com.example.racetransport.transportationrequest.service.RequestService;
import com.example.racetransport.trip.dto.TripDtos.CheckpointRequest;
import com.example.racetransport.trip.dto.TripDtos.CreateTripRequest;
import com.example.racetransport.trip.dto.TripDtos.StaffAssignmentInput;
import com.example.racetransport.trip.dto.TripDtos.StatusUpdateRequest;
import com.example.racetransport.trip.dto.TripDtos.TripDetail;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.service.TripExecutionService;
import com.example.racetransport.trip.service.TripService;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.CustomerProfile;
import com.example.racetransport.user.entity.CustomerType;
import com.example.racetransport.user.entity.EmployeeProfile;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.entity.StaffRole;
import com.example.racetransport.user.repository.AppUserRepository;
import com.example.racetransport.user.repository.CustomerProfileRepository;
import com.example.racetransport.user.repository.EmployeeProfileRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Seed dữ liệu DEMO cho profile dev – chỉ chạy khi bảng app_user trống.
 * Dữ liệu được tạo bằng CHÍNH các service nghiệp vụ (đi qua đầy đủ validation, state machine,
 * lịch sử và thông báo); đồng hồ ứng dụng được "đặt giờ" tạm thời để có dữ liệu lịch sử.
 * KHÔNG dùng ở môi trường thật.
 */
@Component
@Profile("dev")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    public static final String DEMO_PASSWORD = "Demo@123";

    private final AppProperties props;
    private final AppClock clock;
    private final TransactionTemplate tx;
    private final AppUserRepository users;
    private final CustomerProfileRepository customers;
    private final EmployeeProfileRepository employees;
    private final PasswordEncoder encoder;
    private final CodeGenerator codes;
    private final HorseService horseService;
    private final RequestService requestService;
    private final DocumentService documentService;
    private final ComplianceService complianceService;
    private final QuoteService quoteService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final VehicleService vehicleService;
    private final RouteService routeService;
    private final TripService tripService;
    private final TripExecutionService tripExecution;
    private final HealthService healthService;
    private final IncidentService incidentService;
    private final HandoverService handoverService;
    private final ClaimService claimService;
    private final AttachmentService attachmentService;

    private final Map<String, UserPrincipal> who = new HashMap<>();
    private final Map<String, Long> horseIds = new HashMap<>();
    private final Map<String, Long> vehicleIds = new HashMap<>();
    private final Map<String, Long> routeIds = new HashMap<>();
    private final Map<String, Long> stallIds = new HashMap<>();
    private LocalDateTime base;

    public DemoDataSeeder(AppProperties props, AppClock clock, PlatformTransactionManager txManager,
                          AppUserRepository users, CustomerProfileRepository customers,
                          EmployeeProfileRepository employees, PasswordEncoder encoder, CodeGenerator codes,
                          HorseService horseService, RequestService requestService, DocumentService documentService,
                          ComplianceService complianceService, QuoteService quoteService,
                          InvoiceService invoiceService, PaymentService paymentService, VehicleService vehicleService,
                          RouteService routeService, TripService tripService, TripExecutionService tripExecution,
                          HealthService healthService, IncidentService incidentService,
                          HandoverService handoverService, ClaimService claimService,
                          AttachmentService attachmentService) {
        this.props = props;
        this.clock = clock;
        this.tx = new TransactionTemplate(txManager);
        this.users = users;
        this.customers = customers;
        this.employees = employees;
        this.encoder = encoder;
        this.codes = codes;
        this.horseService = horseService;
        this.requestService = requestService;
        this.documentService = documentService;
        this.complianceService = complianceService;
        this.quoteService = quoteService;
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.vehicleService = vehicleService;
        this.routeService = routeService;
        this.tripService = tripService;
        this.tripExecution = tripExecution;
        this.healthService = healthService;
        this.incidentService = incidentService;
        this.handoverService = handoverService;
        this.claimService = claimService;
        this.attachmentService = attachmentService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.seed().demoData()) {
            return;
        }
        if (users.count() > 0) {
            log.info("Demo seed: bỏ qua vì database đã có người dùng");
            return;
        }
        base = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        log.warn("=== Đang seed DỮ LIỆU DEMO (profile dev) – không dùng cho môi trường thật ===");
        try {
            step("Tài khoản demo 5 role", this::seedUsers);
            step("Đội xe, tuyến đường, ngựa", this::seedMasterData);
            step("REQ-0152: chuyến đã giao + sự cố + bồi thường đang thẩm định", this::scenarioCompletedTrip);
            step("REQ-0155: chuyến đang chạy có sự cố NGHIÊM TRỌNG chờ quyết định", this::scenarioEmergencyTrip);
            step("REQ-0157: đã đặt cọc, chờ lập chuyến", this::scenarioReadyForPlanning);
            step("REQ-0158: hồ sơ đầy đủ, chờ Manager phê duyệt", this::scenarioPendingApproval);
            step("REQ-0159: chuyến sắp khởi hành (Driver demo)", this::scenarioScheduledTrip);
            step("REQ-0160: báo giá cuối chờ khách phản hồi", this::scenarioQuoteSent);
            step("REQ-0164 / 0165 / 0166: nháp, chờ rà soát, cần bổ sung", this::scenarioEarlyRequests);
        } finally {
            clock.reset();
        }
        log.warn("=== Seed demo hoàn tất. Mật khẩu mọi tài khoản demo: {} ===", DEMO_PASSWORD);
    }

    private void step(String name, Runnable body) {
        try {
            tx.executeWithoutResult(status -> body.run());
            log.info("Demo seed OK: {}", name);
        } catch (RuntimeException ex) {
            log.error("Demo seed THẤT BẠI ở bước '{}': {}", name, ex.getMessage(), ex);
        } finally {
            clock.reset();
        }
    }

    private void at(LocalDateTime t) {
        clock.setNow(t);
    }

    private LocalDateTime days(int d, int hour, int minute) {
        return base.toLocalDate().plusDays(d).atTime(hour, minute);
    }

    private LocalDateTime hours(long h) {
        return base.plusHours(h);
    }

    private LocalDate date(int d) {
        return base.toLocalDate().plusDays(d);
    }

    private void number(String prefix, long next) {
        codes.ensureAtLeast(prefix, Year.now(clock).getValue(), next);
    }

    // ================================================================== users
    private void seedUsers() {
        customer("customer", "customer@irtms.vn", "Trần Thu Hà", "0903 118 226", "Sao Mai Racing Stable", CustomerType.STABLE,
                "12 Nguyễn Văn Trỗi, Phú Nhuận, TP.HCM");
        customer("customer2", "customer2@irtms.vn", "Lâm Quốc Việt", "0912 456 789", "Phương Nam Equestrian", CustomerType.COMPANY,
                "88 Võ Văn Kiệt, Quận 1, TP.HCM");
        employee("manager", "manager@irtms.vn", "Phạm Quốc Bảo", Role.LOGISTICS_MANAGER, "LM-001", "Điều hành vận tải", null, null);
        employee("specialist", "specialist@irtms.vn", "Lê Thu Trang", Role.TRANSPORT_SPECIALIST, "TS-001", "Hồ sơ & tuân thủ", null, null);
        employee("coordinator", "coordinator@irtms.vn", "Võ Minh Khang", Role.FLEET_ROUTE_COORDINATOR, "FC-001", "Đội xe & tuyến", null, null);
        employee("driver", "driver@irtms.vn", "Lê Hoàng Nam", Role.DRIVER_ESCORT, "DR-001", "Vận hành", StaffRole.DRIVER, "79C-118226");
        employee("escort", "escort@irtms.vn", "Nguyễn Minh Anh", Role.DRIVER_ESCORT, "ES-001", "Vận hành", StaffRole.ESCORT, null);
        employee("driver2", "driver2@irtms.vn", "Trần Văn Lộc", Role.DRIVER_ESCORT, "DR-002", "Vận hành", StaffRole.DRIVER, "79C-224466");
        employee("escort2", "escort2@irtms.vn", "Đặng Thu Hương", Role.DRIVER_ESCORT, "ES-002", "Vận hành", StaffRole.TEAM_LEADER, "79C-335577");
        employee("escort3", "escort3@irtms.vn", "Hồ Gia Huy", Role.DRIVER_ESCORT, "ES-003", "Vận hành", StaffRole.ESCORT, null);
    }

    private void customer(String key, String email, String name, String phone, String company, CustomerType type, String address) {
        AppUser u = user(email, name, phone, Role.CUSTOMER);
        CustomerProfile p = new CustomerProfile();
        p.setUser(u);
        p.setCompanyName(company);
        p.setCustomerType(type);
        p.setAddress(address);
        p.setCountry("VN");
        customers.save(p);
        who.put(key, UserPrincipal.from(u));
    }

    private void employee(String key, String email, String name, Role role, String code, String dept, StaffRole staffRole,
                          String license) {
        AppUser u = user(email, name, "0900 000 " + code.substring(code.length() - 3), role);
        EmployeeProfile p = new EmployeeProfile();
        p.setUser(u);
        p.setEmployeeCode(code);
        p.setDepartment(dept);
        p.setStaffRole(staffRole);
        p.setLicenseNumber(license);
        p.setLicenseExpiry(license == null ? null : LocalDate.of(2029, 6, 30));
        p.setAirEscortCertified(staffRole == StaffRole.ESCORT || staffRole == StaffRole.TEAM_LEADER);
        p.setEmergencyContact("Phòng điều hành IRTMS – 028 3838 0000");
        p.setBaseLocation("TP. Hồ Chí Minh");
        employees.save(p);
        who.put(key, UserPrincipal.from(u));
    }

    private AppUser user(String email, String name, String phone, Role role) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setFullName(name);
        u.setPhone(phone);
        u.setRole(role);
        u.setEnabled(true);
        u.setPasswordHash(encoder.encode(DEMO_PASSWORD));
        return users.save(u);
    }

    private UserPrincipal p(String key) {
        UserPrincipal u = who.get(key);
        if (u == null) {
            AppUser user = users.findByEmailIgnoreCase(key + "@irtms.vn").orElseThrow();
            u = UserPrincipal.from(user);
            who.put(key, u);
        }
        return u;
    }

    // ================================================================== master data
    private void seedMasterData() {
        at(days(-60, 9, 0));
        for (StallTypeResponse s : horseService.stallTypes()) {
            stallIds.put(s.code(), s.id());
        }
        UserPrincipal co = p("coordinator");
        vehicleIds.put("HT-01", vehicle(co, "HT-01", "51C-118.26", VehicleType.HORSE_TRUCK, "Hino 500 – thùng 4 ngăn", 4, true));
        vehicleIds.put("HT-07", vehicle(co, "HT-07", "51C-707.07", VehicleType.HORSE_TRUCK, "Isuzu FVR – thùng 6 ngăn, điều hòa", 6, true));
        vehicleIds.put("HT-03", vehicle(co, "HT-03", "51R-030.33", VehicleType.HORSE_TRAILER, "Rơ-moóc 2 ngăn", 2, false));
        vehicleIds.put("AS-01", vehicle(co, "AS-01", "AIR-STALL-01", VehicleType.AIR_STALL, "Chuồng bay 3 ngăn (IATA)", 3, true));
        vehicleIds.put("SV-02", vehicle(co, "SV-02", "51A-220.02", VehicleType.SUPPORT_VEHICLE, "Xe hỗ trợ, dụng cụ thú y sơ cứu", 1, true));
        vehicleService.changeStatus(vehicleIds.get("HT-03"), VehicleStatus.MAINTENANCE, "Bảo dưỡng định kỳ hệ thống phanh");

        routeIds.put("BKK", route(co, "RT-HCM-BKK", "TP.HCM → Bangkok (qua Mộc Bài – Poipet)", "TH", "Bangkok, Thái Lan",
                TransportMode.ROAD, "960", "26", null, List.of(
                        stop(StopType.PICKUP, "Trại Sao Mai, Củ Chi", "VN", "10.9733", "106.4931", 0, 60, "Kiểm tra trước vận chuyển, xếp ngựa lên xe"),
                        stop(StopType.BORDER, "Cửa khẩu Mộc Bài – Bavet", "VN", "11.0790", "106.1720", 150, 60, "Xuất trình hồ sơ xuất khẩu, kiểm dịch"),
                        stop(StopType.REST_STOP, "Điểm nghỉ Phnom Penh", "KH", "11.5564", "104.9282", 420, 90, "Cho ngựa uống nước, nghỉ 90 phút"),
                        stop(StopType.BORDER, "Cửa khẩu Poipet – Aranyaprathet", "KH", "13.6600", "102.5560", 840, 120, "Thủ tục hải quan Thái Lan"),
                        stop(StopType.REST_STOP, "Trạm nghỉ Sa Kaeo", "TH", "13.8240", "102.0640", 1020, 60, null),
                        stop(StopType.DESTINATION, "Royal Bangkok Sports Club", "TH", "13.7400", "100.5380", 1440, 60, "Bàn giao cho đại diện câu lạc bộ"))));
        routeIds.put("BKK-ALT", route(co, "RT-HCM-BKK-S", "TP.HCM → Bangkok (tuyến phía Nam qua Hà Tiên – Hat Lek)", "TH",
                "Bangkok, Thái Lan", TransportMode.ROAD, "1080", "30", routeIds.get("BKK"), List.of(
                        stop(StopType.PICKUP, "Trại Sao Mai, Củ Chi", "VN", "10.9733", "106.4931", 0, 60, null),
                        stop(StopType.BORDER, "Cửa khẩu Hà Tiên – Prek Chak", "VN", "10.4220", "104.4880", 360, 90, null),
                        stop(StopType.BORDER, "Cửa khẩu Koh Kong – Hat Lek", "KH", "11.6100", "102.9100", 720, 120, null),
                        stop(StopType.REST_STOP, "Trạm nghỉ Chanthaburi", "TH", "12.6100", "102.1040", 960, 60, null),
                        stop(StopType.DESTINATION, "Royal Bangkok Sports Club", "TH", "13.7400", "100.5380", 1680, 60, null))));
        routeIds.put("PNH", route(co, "RT-HCM-PNH", "TP.HCM → Phnom Penh", "KH", "Phnom Penh, Campuchia",
                TransportMode.ROAD, "240", "8", null, List.of(
                        stop(StopType.PICKUP, "Trại Sao Mai, Củ Chi", "VN", "10.9733", "106.4931", 0, 45, null),
                        stop(StopType.BORDER, "Cửa khẩu Mộc Bài – Bavet", "VN", "11.0790", "106.1720", 150, 60, null),
                        stop(StopType.CUSTOMS, "Hải quan Bavet", "KH", "11.0830", "106.1500", 220, 45, null),
                        stop(StopType.DESTINATION, "Trung tâm huấn luyện ngựa Phnom Penh", "KH", "11.5564", "104.9282", 420, 45, null))));
        routeIds.put("SG-AIR", route(co, "RT-HCM-SIN-AIR", "TP.HCM → Singapore (hàng không)", "SG", "Kranji, Singapore",
                TransportMode.ROAD_AIR, "1100", "14", null, List.of(
                        stop(StopType.PICKUP, "Trại Sao Mai, Củ Chi", "VN", "10.9733", "106.4931", 0, 60, null),
                        stop(StopType.AIRPORT, "Sân bay Tân Sơn Nhất – kho hàng động vật", "VN", "10.8188", "106.6520", 120, 180, "Đóng air stall, kiểm dịch xuất"),
                        stop(StopType.AIRPORT, "Sân bay Changi – Animal Holding", "SG", "1.3644", "103.9915", 420, 120, null),
                        stop(StopType.QUARANTINE, "Trạm cách ly ngựa Kranji", "SG", "1.4250", "103.7560", 600, 0, "Cách ly theo yêu cầu nước nhập"),
                        stop(StopType.DESTINATION, "Singapore Turf Club, Kranji", "SG", "1.4240", "103.7590", 720, 60, null))));
        routeIds.put("KUL", route(co, "RT-HCM-KUL", "TP.HCM → Kuala Lumpur (đường bộ)", "MY", "Kuala Lumpur, Malaysia",
                TransportMode.ROAD, "1900", "52", null, List.of(
                        stop(StopType.PICKUP, "Kho ngựa Phương Nam, Bình Chánh", "VN", "10.7200", "106.6100", 0, 60, null),
                        stop(StopType.BORDER, "Cửa khẩu Mộc Bài – Bavet", "VN", "11.0790", "106.1720", 150, 60, null),
                        stop(StopType.BORDER, "Cửa khẩu Poipet – Aranyaprathet", "KH", "13.6600", "102.5560", 840, 120, null),
                        stop(StopType.STABLE, "Chuồng trung chuyển Hua Hin", "TH", "12.5680", "99.9570", 1500, 480, "Nghỉ qua đêm"),
                        stop(StopType.BORDER, "Cửa khẩu Sadao – Bukit Kayu Hitam", "TH", "6.5180", "100.4200", 2400, 120, null),
                        stop(StopType.DESTINATION, "Selangor Turf Club", "MY", "3.0430", "101.6760", 3060, 60, null))));

        at(days(-58, 10, 0));
        UserPrincipal c1 = p("customer");
        UserPrincipal c2 = p("customer2");
        horse(c1, "Silver Arrow", "Thoroughbred", LocalDate.of(2020, 3, 14), HorseSex.GELDING, "Xám bạc", 166, "985112004517330", "VN-HP-2020-0331");
        horse(c1, "Golden Lotus", "Thoroughbred", LocalDate.of(2019, 9, 2), HorseSex.MARE, "Hạt dẻ", 162, "985112004517331", "VN-HP-2019-0902");
        horse(c1, "Midnight Star", "Arabian", LocalDate.of(2018, 5, 21), HorseSex.STALLION, "Đen", 158, "985112004517332", "VN-HP-2018-0521");
        horse(c1, "Ocean Breeze", "Thoroughbred", LocalDate.of(2022, 1, 9), HorseSex.FILLY, "Nâu", 155, "985112004517333", "VN-HP-2022-0109");
        horse(c1, "Royal Phoenix", "Warmblood", LocalDate.of(2017, 7, 30), HorseSex.STALLION, "Hồng mã", 170, "985112004517334", "VN-HP-2017-0730");
        horse(c2, "River Dancer", "Thoroughbred", LocalDate.of(2019, 4, 11), HorseSex.MARE, "Nâu đỏ", 163, "985112004518801", "VN-HP-2019-0411");
        horse(c2, "Thunder Bay", "Thoroughbred", LocalDate.of(2020, 11, 5), HorseSex.GELDING, "Hạt dẻ", 167, "985112004518802", "VN-HP-2020-1105");
        horse(c2, "Jade Empress", "Arabian", LocalDate.of(2016, 2, 18), HorseSex.MARE, "Trắng xám", 152, "985112004518803", "VN-HP-2016-0218");
        horse(c2, "Lucky Comet", "Thoroughbred", LocalDate.of(2021, 6, 6), HorseSex.COLT, "Nâu", 160, "985112004518804", "VN-HP-2021-0606");
        horse(c2, "Morning Glory", "Thoroughbred", LocalDate.of(2020, 8, 23), HorseSex.MARE, "Hạt dẻ", 161, "985112004518805", "VN-HP-2020-0823");
    }

    private Long vehicle(UserPrincipal co, String code, String plate, VehicleType type, String model, int cap, boolean climate) {
        VehicleResponse v = vehicleService.create(new VehicleRequest(code, plate, type, model, cap, "Kho xe Củ Chi, TP.HCM",
                climate, true, date(-90), date(120), null));
        return v.id();
    }

    private RouteStopRequest stop(StopType type, String name, String country, String lat, String lng, int offsetMin,
                                  int durationMin, String instructions) {
        return new RouteStopRequest(null, type, name, null, country, new BigDecimal(lat), new BigDecimal(lng), offsetMin,
                durationMin, instructions);
    }

    private Long route(UserPrincipal co, String code, String name, String destCountry, String dest, TransportMode mode,
                       String km, String hoursEst, Long alternativeOf, List<RouteStopRequest> stops) {
        RouteDetail r = routeService.create(new RouteRequest(code, name, "VN", "TP. Hồ Chí Minh", destCountry, dest, mode,
                new BigDecimal(km), new BigDecimal(hoursEst), RouteStatus.ACTIVE, alternativeOf, null, stops));
        return r.id();
    }

    private void horse(UserPrincipal owner, String name, String breed, LocalDate dob, HorseSex sex, String color, int height,
                       String chip, String passport) {
        HorseResponse h = horseService.create(owner, new HorseRequest(name, breed, dob, sex, color, height, chip, passport, "VN",
                null, "Dữ liệu demo"));
        horseIds.put(name, h.id());
    }

    // ================================================================== request helpers
    private RequestHorseInput rh(String horse, String stall, String value) {
        return new RequestHorseInput(horseIds.get(horse), stallIds.get(stall), new BigDecimal(value), null);
    }

    private RequestDetail createRequest(String customerKey, long number, String originAddr, String destCountry,
                                        String destAddr, LocalDate departure, LocalDate returnDate, MovementType movement,
                                        TransportPurpose purpose, TransportMode mode, String special,
                                        List<RequestHorseInput> horses) {
        number(CodeGenerator.REQUEST, number);
        return requestService.create(p(customerKey), new RequestUpsert("VN", originAddr, destCountry, destAddr, departure,
                returnDate, movement, purpose, mode, "USD", special, null, horses));
    }

    /** Khách tải một tài liệu cho mỗi mục checklist bắt buộc còn mở (trừ những loại trong `skip`). */
    private List<DocumentResponse> uploadAll(String customerKey, Long requestId, LocalDate issueDate, List<DocumentType> skip) {
        List<DocumentResponse> out = new ArrayList<>();
        List<ChecklistItemResponse> items = complianceService.getChecklist(p(customerKey), requestId).items();
        for (ChecklistItemResponse it : items) {
            if (!it.mandatory() || it.status() != ChecklistItemStatus.MISSING || skip.contains(it.documentType())) {
                continue;
            }
            String subject = it.horseName() == null ? "Lo ngua" : it.horseName();
            String fileName = it.documentType().name().toLowerCase() + "_" + DemoFiles.ascii(subject).replace(' ', '_') + ".pdf";
            out.add(documentService.upload(p(customerKey), DemoFiles.pdf(fileName, it.title(), "Doi tuong: " + subject,
                            "Ngay cap: " + issueDate), it.documentType(), requestId, it.horseId(), it.id(), it.title(),
                    "DEMO-" + it.documentType().name().substring(0, 3) + "-" + (1000 + it.id()), "VN", issueDate,
                    issueDate.plusYears(1)));
        }
        return out;
    }

    private void approveAllDocuments(Long requestId) {
        for (DocumentResponse d : documentService.listForRequest(p("specialist"), requestId)) {
            if (d.reviewStatus() == DocumentReviewStatus.PENDING_REVIEW) {
                documentService.review(p("specialist"), d.id(), new ReviewRequest(DocumentReviewStatus.APPROVED,
                        "Hợp lệ – đã đối chiếu số microchip/hộ chiếu"));
            }
        }
    }

    /** Specialist rà soát xong và chuyển Manager. */
    private void specialistComplete(Long requestId) {
        UserPrincipal sp = p("specialist");
        approveAllDocuments(requestId);
        requestService.completeDocumentReview(sp, requestId, new DecisionRequest("Đã rà soát toàn bộ tài liệu"));
        requestService.completeCompliance(sp, requestId, new DecisionRequest("Checklist tuân thủ đạt đầy đủ (dữ liệu demo)"));
    }

    /** Manager lập báo giá cuối: xác nhận toàn bộ dòng ước tính, có thể thêm dòng điều chỉnh, rồi gửi khách. */
    private QuoteResponse buildAndSendQuote(Long requestId, List<QuoteItemInput> extra, String notes) {
        UserPrincipal m = p("manager");
        QuoteResponse draft = quoteService.createFinal(m, requestId);
        List<QuoteItemInput> items = new ArrayList<>();
        for (QuoteItemResponse i : draft.items()) {
            items.add(new QuoteItemInput(i.category(), i.description(), i.quantity(), i.unitPrice(), true,
                    i.source() == null ? "Xác nhận chi phí thực" : i.source(), i.estimatedAmount()));
        }
        items.addAll(extra);
        quoteService.update(m, draft.id(), new QuoteUpdateRequest(null, notes, items));
        return quoteService.send(m, draft.id());
    }

    private QuoteItemInput extra(CostCategory c, String desc, String qty, String unit) {
        return new QuoteItemInput(c, desc, new BigDecimal(qty), new BigDecimal(unit), true, "Điều chỉnh của Manager", null);
    }

    private void pay(String customerKey, Long invoiceId, BigDecimal amount, PaymentMethod method, String last4) {
        paymentService.simulate(p(customerKey), new SimulatePaymentRequest(invoiceId, amount, method, last4, "Thanh toán demo"));
    }

    private Long stopId(RouteDetail r, int order) {
        return r.stops().stream().filter(s -> s.stopOrder() == order).map(RouteStopResponse::id).findFirst().orElseThrow();
    }

    private void checkpoint(String driverKey, Long tripId, Long stopId, TrackingEventType type, String note) {
        tripExecution.checkpoint(p(driverKey), tripId, new CheckpointRequest(stopId, type, null, null, null, null, note, null, null));
    }

    private void preCheck(String driverKey, Long tripId, String horse, String temp, String note) {
        healthService.recordPreTransport(p(driverKey), new PreTransportCheckRequest(tripId, horseIds.get(horse), new BigDecimal(temp),
                HorseCondition.GOOD, true, true, true, true, note));
    }

    // ================================================================== scenarios
    /** Yêu cầu đã hoàn tất: chuyến TRIP-0087 đã giao, sự cố INC-0052 (mức Trung bình) và hồ sơ bồi thường CLM-0014. */
    private void scenarioCompletedTrip() {
        at(days(-45, 9, 0));
        RequestDetail r = createRequest("customer", 152, "Trại Sao Mai, Củ Chi, TP.HCM", "TH",
                "Royal Bangkok Sports Club, Bangkok", date(-30), date(-16), MovementType.TEMPORARY, TransportPurpose.COMPETITION,
                TransportMode.ROAD, "Ngựa quen di chuyển đường dài; cần nghỉ mỗi 4–5 giờ.",
                List.of(rh("Midnight Star", "STANDARD", "180000")));
        requestService.submit(p("customer"), r.id());
        at(days(-36, 9, 0));
        uploadAll("customer", r.id(), date(-37), List.of());
        at(days(-35, 10, 0));
        requestService.startDocumentReview(p("specialist"), r.id());
        specialistComplete(r.id());
        at(days(-34, 9, 0));
        requestService.approve(p("manager"), r.id(), new DecisionRequest("Đủ điều kiện vận hành"));
        QuoteResponse q = buildAndSendQuote(r.id(), List.of(), "Đã xác nhận chi phí với đối tác tại Bangkok.");
        at(days(-33, 14, 0));
        quoteService.accept(p("customer"), q.id(), new RespondRequest("Đồng ý"));
        Long invoiceId = quoteService.get(p("customer"), q.id()).invoiceId();
        pay("customer", invoiceId, invoiceService.get(p("customer"), invoiceId).totalAmount(), PaymentMethod.SIMULATED_BANK_TRANSFER, null);

        at(days(-32, 9, 0));
        number(CodeGenerator.TRIP, 87);
        RouteDetail route = routeService.get(routeIds.get("BKK"));
        TripDetail t = tripService.create(p("coordinator"), new CreateTripRequest(r.id(), route.id(), days(-30, 6, 0), days(-29, 9, 0),
                vehicleIds.get("HT-07"), "Kiểm tra nước uống mỗi điểm dừng; ngựa nhạy cảm tiếng còi.", null,
                List.of(new StaffAssignmentInput(p("driver").getId(), StaffRole.DRIVER),
                        new StaffAssignmentInput(p("escort").getId(), StaffRole.ESCORT))));
        at(days(-30, 5, 15));
        preCheck("escort", t.id(), "Midnight Star", "37.8", "Ăn uống bình thường, lên xe thuận lợi");
        at(days(-30, 6, 5));
        tripExecution.start(p("driver"), t.id());
        at(days(-30, 8, 40));
        checkpoint("driver", t.id(), stopId(route, 2), TrackingEventType.CHECKPOINT_ARRIVED, "Làm thủ tục xuất cảnh");
        at(days(-30, 9, 50));
        checkpoint("driver", t.id(), stopId(route, 2), TrackingEventType.CHECKPOINT_DEPARTED, null);
        at(days(-30, 13, 10));
        checkpoint("driver", t.id(), stopId(route, 3), TrackingEventType.CHECKPOINT_ARRIVED, "Nghỉ, cho ngựa uống nước");
        at(days(-30, 13, 30));
        healthService.recordObservation(p("escort"), new ObservationRequest(t.id(), horseIds.get("Midnight Star"), null,
                "Điểm nghỉ Phnom Penh", IntakeLevel.NORMAL, IntakeLevel.NORMAL, HorseBehavior.CALM, StressLevel.LOW,
                MovementCondition.NORMAL, false, null, new BigDecimal("37.9"), null, "Bình thường", false));
        at(days(-30, 14, 45));
        checkpoint("driver", t.id(), stopId(route, 3), TrackingEventType.CHECKPOINT_DEPARTED, null);
        at(days(-30, 18, 20));
        tripExecution.checkpoint(p("driver"), t.id(), new CheckpointRequest(null, TrackingEventType.DELAY, "QL5, Battambang",
                null, null, 45, "Đường đang sửa, giảm tốc độ", null, null));
        at(days(-30, 18, 35));
        IncidentDetail inc = incidentReport("escort", t.id(), "Midnight Star", 52, IncidentType.HORSE_INJURY, IncidentSeverity.MEDIUM,
                "Trầy xước chân sau trái khi xe phanh gấp",
                "Xe phanh gấp tránh xe máy; Midnight Star bị trầy da cổ chân sau trái ~4cm, đi lại bình thường. Đã sát trùng tại chỗ.",
                "QL5, Battambang");
        at(days(-30, 19, 10));
        incidentService.addAction(p("manager"), inc.id(), new IncidentActionRequest(IncidentActionType.NOTE,
                "Theo dõi vết thương mỗi 2 giờ, gửi ảnh cập nhật. Liên hệ bác sĩ thú y tại Bangkok khi tới nơi.", null, null, null, null, null));
        tripExecution.updateStatus(p("driver"), t.id(), new StatusUpdateRequest(TripStatus.IN_TRANSIT, "Tiếp tục hành trình", null, null, null, null));
        at(days(-29, 0, 30));
        checkpoint("driver", t.id(), stopId(route, 4), TrackingEventType.CHECKPOINT_ARRIVED, "Thủ tục hải quan Thái Lan");
        at(days(-29, 2, 15));
        checkpoint("driver", t.id(), stopId(route, 4), TrackingEventType.CHECKPOINT_DEPARTED, "Hoàn tất thông quan");
        at(days(-29, 9, 40));
        tripExecution.updateStatus(p("driver"), t.id(), new StatusUpdateRequest(TripStatus.ARRIVED, "Đến RBSC",
                "Royal Bangkok Sports Club", null, null, null));
        at(days(-29, 10, 0));
        healthService.recordConsultation(p("manager"), new VetConsultationRequest(inc.id(), null, null, "BS. Somchai Prasert",
                "Bangkok Equine Hospital", "+66 2 000 1234", days(-29, 9, 55),
                "Vết trầy nông, không ảnh hưởng gân. Vệ sinh 2 lần/ngày, theo dõi 5 ngày.", OperationalDecision.MONITOR));
        incidentService.changeStatus(p("manager"), inc.id(), IncidentStatus.RESOLVED, "Đã điều trị theo tư vấn thú y bên ngoài");
        healthService.recordArrival(p("escort"), new ArrivalCheckRequest(t.id(), horseIds.get("Midnight Star"), HorseCondition.FAIR,
                true, "Vết trầy đã khô, ngựa ăn uống bình thường"));
        HandoverResponse h = handoverService.create(p("escort"), new HandoverRequest(t.id(), horseIds.get("Midnight Star"), null,
                null, "Somsak Wongchai", "+66 81 234 5678", HorseCondition.FAIR, true, "Bàn giao kèm hồ sơ và thuốc sát trùng"));
        attachmentService.upload(p("escort"), AttachmentRefType.HANDOVER, h.id(), "Ảnh bàn giao",
                DemoFiles.png("handover_midnight_star.png", "Ban giao Midnight Star - RBSC"));
        at(days(-29, 12, 0));
        handoverService.confirm(p("customer"), h.id(), new ConfirmHandoverRequest(true, "Đã nhận ngựa, cảm ơn đội vận hành"));

        at(days(-27, 10, 0));
        number(CodeGenerator.CLAIM, 14);
        var claim = claimService.create(p("customer"), new CreateClaimRequest(r.id(), t.id(), inc.id(), horseIds.get("Midnight Star"),
                ClaimType.INJURY_MEDICAL_EXPENSE, new BigDecimal("6500"),
                "Chi phí điều trị vết thương chân và nghỉ thi đấu 5 ngày theo chỉ định của bác sĩ thú y tại Bangkok."));
        claimService.addEvidence(p("customer"), claim.id(), DemoFiles.pdf("hoa_don_thu_y.pdf", "Hoa don dieu tri", "Tong: 6,500 USD"),
                EvidenceType.RECEIPT, "Hóa đơn bệnh viện thú y");
        claimService.addEvidence(p("customer"), claim.id(), DemoFiles.png("vet_thuong.png", "Vet tray co chan sau trai"),
                EvidenceType.PHOTO, "Ảnh vết thương");
        at(days(-25, 15, 0));
        claimService.review(p("manager"), claim.id(), new ReviewClaimRequest(
                "Chi phí điều trị hợp lệ; khoản 800 USD phí lưu chuồng thêm không thuộc phạm vi bồi thường.",
                ClaimRecommendation.PARTIALLY_APPROVE, new BigDecimal("5700")));
    }

    private IncidentDetail incidentReport(String reporterKey, Long tripId, String horse, long number, IncidentType type,
                                          IncidentSeverity severity, String title, String desc, String location) {
        number(CodeGenerator.INCIDENT, number);
        return incidentService.report(p(reporterKey), new ReportIncidentRequest(tripId, horse == null ? null : horseIds.get(horse),
                type, severity, title, desc, location, null));
    }

    /** Chuyến TRIP-0094 đang chạy, dừng tại Poipet vì sự cố NGHIÊM TRỌNG INC-0053, đã có tư vấn thú y, chờ Manager. */
    private void scenarioEmergencyTrip() {
        at(days(-20, 10, 0));
        RequestDetail r = createRequest("customer2", 155, "Kho ngựa Phương Nam, Bình Chánh, TP.HCM", "TH",
                "Royal Bangkok Sports Club, Bangkok", date(-1), date(20), MovementType.TEMPORARY, TransportPurpose.RACING,
                TransportMode.ROAD, null, List.of(rh("River Dancer", "WIDE", "250000"), rh("Thunder Bay", "STANDARD", "210000")));
        requestService.submit(p("customer2"), r.id());
        at(days(-6, 9, 0));
        uploadAll("customer2", r.id(), date(-7), List.of());
        at(days(-6, 15, 0));
        requestService.startDocumentReview(p("specialist"), r.id());
        specialistComplete(r.id());
        at(days(-5, 9, 0));
        requestService.approve(p("manager"), r.id(), null);
        QuoteResponse q = buildAndSendQuote(r.id(), List.of(extra(CostCategory.SURCHARGE, "Phụ phí xuất phát sáng sớm", "1", "150")), null);
        at(days(-4, 10, 0));
        quoteService.accept(p("customer2"), q.id(), null);
        Long invoiceId = quoteService.get(p("customer2"), q.id()).invoiceId();
        pay("customer2", invoiceId, invoiceService.get(p("customer2"), invoiceId).depositAmount(), PaymentMethod.SIMULATED_CARD, "4242");

        at(days(-4, 11, 0));
        number(CodeGenerator.TRIP, 94);
        RouteDetail route = routeService.get(routeIds.get("BKK"));
        TripDetail t = tripService.create(p("coordinator"), new CreateTripRequest(r.id(), route.id(), hours(-20), hours(8),
                vehicleIds.get("HT-01"), "River Dancer dễ căng thẳng: hạn chế mở cửa thùng xe ở cửa khẩu.", null,
                List.of(new StaffAssignmentInput(p("driver2").getId(), StaffRole.DRIVER),
                        new StaffAssignmentInput(p("escort2").getId(), StaffRole.TEAM_LEADER),
                        new StaffAssignmentInput(p("escort3").getId(), StaffRole.ESCORT))));
        at(hours(-21));
        preCheck("escort2", t.id(), "River Dancer", "37.7", null);
        preCheck("escort2", t.id(), "Thunder Bay", "37.6", null);
        at(hours(-20));
        tripExecution.start(p("driver2"), t.id());
        at(hours(-18));
        checkpoint("driver2", t.id(), stopId(route, 2), TrackingEventType.CHECKPOINT_ARRIVED, null);
        at(hours(-17));
        checkpoint("driver2", t.id(), stopId(route, 2), TrackingEventType.CHECKPOINT_DEPARTED, null);
        at(hours(-13));
        checkpoint("driver2", t.id(), stopId(route, 3), TrackingEventType.CHECKPOINT_ARRIVED, null);
        at(hours(-12).plusMinutes(30));
        healthService.recordObservation(p("escort3"), new ObservationRequest(t.id(), horseIds.get("River Dancer"), null, null,
                IntakeLevel.REDUCED, IntakeLevel.NORMAL, HorseBehavior.RESTLESS, StressLevel.MEDIUM, MovementCondition.NORMAL,
                false, null, new BigDecimal("38.1"), null, "Ăn ít hơn bình thường", false));
        at(hours(-11));
        checkpoint("driver2", t.id(), stopId(route, 3), TrackingEventType.CHECKPOINT_DEPARTED, null);
        at(hours(-6));
        checkpoint("driver2", t.id(), stopId(route, 4), TrackingEventType.CHECKPOINT_ARRIVED, "Chờ thông quan Poipet");
        at(hours(-4));
        ObservationResponse obs = healthService.recordObservation(p("escort2"), new ObservationRequest(t.id(), horseIds.get("River Dancer"),
                null, "Cửa khẩu Poipet", IntakeLevel.NONE, IntakeLevel.REDUCED, HorseBehavior.AGITATED, StressLevel.HIGH,
                MovementCondition.STIFF, false, null, new BigDecimal("38.9"), null,
                "Cào chân, nhìn hông liên tục, không ăn – nghi đau bụng (chỉ là quan sát)", false));
        attachmentService.upload(p("escort2"), AttachmentRefType.HEALTH_OBSERVATION, obs.id(), "Ảnh quan sát",
                DemoFiles.png("river_dancer_poipet.png", "River Dancer - Poipet"));
        at(hours(-4).plusMinutes(10));
        IncidentDetail inc = incidentReport("escort2", t.id(), "River Dancer", 53, IncidentType.HORSE_HEALTH, IncidentSeverity.CRITICAL,
                "River Dancer có dấu hiệu đau bụng tại cửa khẩu Poipet",
                "Ngựa bỏ ăn, cào chân, nhìn hông, nhiệt độ 38.9°C. Đội áp tải đã dừng xe ở bãi chờ, giữ ngựa đi bộ nhẹ. "
                        + "Cần quyết định có tiếp tục hành trình hay không.", "Cửa khẩu Poipet");
        at(hours(-3).plusMinutes(40));
        incidentService.addAction(p("escort2"), inc.id(), new IncidentActionRequest(IncidentActionType.VET_CONTACTED,
                "Đã gọi BS. Chanida (Sa Kaeo Equine Clinic), bác sĩ tới trong 40 phút.", null, null, null, null, null));
        at(hours(-2).plusMinutes(50));
        healthService.recordConsultation(p("escort2"), new VetConsultationRequest(inc.id(), null, null, "BS. Chanida Wattana",
                "Sa Kaeo Equine Clinic", "+66 37 000 555", hours(-3),
                "Đau bụng mức nhẹ, nhu động ruột có. Đã truyền dịch, khuyến nghị nghỉ 6 giờ và theo dõi trước khi đi tiếp.",
                OperationalDecision.MONITOR));
        incidentService.addAction(p("specialist"), inc.id(), new IncidentActionRequest(IncidentActionType.NOTE,
                "Hồ sơ sức khỏe xuất phát hợp lệ; nếu dừng quá 24h cần xin gia hạn giấy phép quá cảnh.", null, null, null, null, null));
    }

    /** REQ-0157: đã chấp nhận báo giá và đặt cọc – Coordinator demo lập chuyến. */
    private void scenarioReadyForPlanning() {
        at(days(-15, 9, 0));
        RequestDetail r = createRequest("customer2", 157, "Kho ngựa Phương Nam, Bình Chánh, TP.HCM", "MY",
                "Selangor Turf Club, Kuala Lumpur", date(5), null, MovementType.PERMANENT, TransportPurpose.RELOCATION,
                TransportMode.ROAD, "Ngựa cái lớn tuổi – cần chuồng hộp riêng.", List.of(rh("Jade Empress", "BOX", "95000")));
        requestService.submit(p("customer2"), r.id());
        at(days(-3, 9, 0));
        uploadAll("customer2", r.id(), date(-4), List.of());
        at(days(-3, 14, 0));
        requestService.startDocumentReview(p("specialist"), r.id());
        specialistComplete(r.id());
        at(days(-2, 9, 0));
        requestService.approve(p("manager"), r.id(), null);
        QuoteResponse q = buildAndSendQuote(r.id(), List.of(extra(CostCategory.DISCOUNT, "Ưu đãi khách hàng thân thiết", "1", "300")),
                "Giá đã gồm chuồng hộp riêng.");
        at(days(-2, 16, 0));
        quoteService.accept(p("customer2"), q.id(), null);
        Long invoiceId = quoteService.get(p("customer2"), q.id()).invoiceId();
        at(days(-1, 10, 0));
        // lần thử đầu bị từ chối (thẻ 0002) để minh họa luồng lỗi, sau đó chuyển khoản đặt cọc
        pay("customer2", invoiceId, invoiceService.get(p("customer2"), invoiceId).depositAmount(), PaymentMethod.SIMULATED_CARD, "0002");
        pay("customer2", invoiceId, invoiceService.get(p("customer2"), invoiceId).depositAmount(),
                PaymentMethod.SIMULATED_BANK_TRANSFER, null);
    }

    /** REQ-0158: hồ sơ tuân thủ đầy đủ, chờ Manager phê duyệt (Manager demo). */
    private void scenarioPendingApproval() {
        at(days(-10, 9, 30));
        RequestDetail r = createRequest("customer", 158, "Trại Sao Mai, Củ Chi, TP.HCM", "SG", "Singapore Turf Club, Kranji",
                date(6), date(30), MovementType.TEMPORARY, TransportPurpose.RACING, TransportMode.ROAD_AIR,
                "Tham dự Singapore Gold Cup – cần air stall có người áp tải theo ngựa.",
                List.of(rh("Silver Arrow", "STANDARD", "250000"), rh("Golden Lotus", "WIDE", "180000")));
        requestService.submit(p("customer"), r.id());
        at(days(-2, 10, 0));
        uploadAll("customer", r.id(), date(-3), List.of());
        at(days(-1, 9, 0));
        requestService.startDocumentReview(p("specialist"), r.id());
        specialistComplete(r.id());
    }

    /** REQ-0159 / TRIP-0101: chuyến khởi hành trong ~20 giờ, đã phân công driver@ và escort@ (Driver demo). */
    private void scenarioScheduledTrip() {
        at(days(-8, 9, 0));
        RequestDetail r = createRequest("customer", 159, "Trại Sao Mai, Củ Chi, TP.HCM", "KH",
                "Trung tâm huấn luyện ngựa Phnom Penh", date(1), date(15), MovementType.TEMPORARY,
                TransportPurpose.TRAINING, TransportMode.ROAD, null, List.of(rh("Midnight Star", "STANDARD", "180000")));
        requestService.submit(p("customer"), r.id());
        at(days(-1, 8, 0));
        uploadAll("customer", r.id(), date(-2), List.of());
        at(days(-1, 9, 0));
        requestService.startDocumentReview(p("specialist"), r.id());
        specialistComplete(r.id());
        at(days(-1, 10, 0));
        requestService.approve(p("manager"), r.id(), null);
        QuoteResponse q = buildAndSendQuote(r.id(), List.of(), null);
        at(days(-1, 11, 0));
        quoteService.accept(p("customer"), q.id(), null);
        Long invoiceId = quoteService.get(p("customer"), q.id()).invoiceId();
        pay("customer", invoiceId, invoiceService.get(p("customer"), invoiceId).totalAmount(), PaymentMethod.SIMULATED_EWALLET, null);
        at(days(-1, 13, 0));
        number(CodeGenerator.TRIP, 101);
        tripService.create(p("coordinator"), new CreateTripRequest(r.id(), routeIds.get("PNH"), hours(20), hours(30),
                vehicleIds.get("HT-07"), "Chuyến ngắn; kiểm tra giấy tờ cửa khẩu Mộc Bài trước 7h.", null,
                List.of(new StaffAssignmentInput(p("driver").getId(), StaffRole.DRIVER),
                        new StaffAssignmentInput(p("escort").getId(), StaffRole.ESCORT))));
    }

    /** REQ-0160: báo giá cuối đã gửi, chờ khách chấp nhận/từ chối (Customer demo). */
    private void scenarioQuoteSent() {
        at(days(-12, 9, 0));
        RequestDetail r = createRequest("customer", 160, "Trại Sao Mai, Củ Chi, TP.HCM", "SG", "Singapore Turf Club, Kranji",
                date(8), null, MovementType.PERMANENT, TransportPurpose.BREEDING, TransportMode.ROAD_AIR,
                "Ngựa giống – cần hộp riêng và áp tải có chứng chỉ hàng không.", List.of(rh("Royal Phoenix", "BOX", "320000")));
        requestService.submit(p("customer"), r.id());
        at(days(-1, 9, 0));
        uploadAll("customer", r.id(), date(-1), List.of());
        at(days(-1, 14, 0));
        requestService.startDocumentReview(p("specialist"), r.id());
        specialistComplete(r.id());
        at(days(-1, 16, 0));
        requestService.approve(p("manager"), r.id(), null);
        buildAndSendQuote(r.id(), List.of(extra(CostCategory.HANDLING, "Phí đóng air stall riêng cho ngựa giống", "1", "450"),
                extra(CostCategory.DISCOUNT, "Giảm 3% khách hàng thân thiết", "1", "520")), "Báo giá có hiệu lực 14 ngày.");
    }

    /** Nháp, yêu cầu chờ Specialist rà soát và yêu cầu cần bổ sung thông tin. */
    private void scenarioEarlyRequests() {
        at(days(-1, 15, 0));
        createRequest("customer", 164, "Trại Sao Mai, Củ Chi, TP.HCM", "TH", "Chiang Mai Equestrian Centre",
                date(40), date(60), MovementType.TEMPORARY, TransportPurpose.TRAINING, TransportMode.ROAD, null,
                List.of(rh("Ocean Breeze", "STANDARD", "90000")));

        at(days(-2, 10, 0));
        RequestDetail sub = createRequest("customer2", 165, "Kho ngựa Phương Nam, Bình Chánh, TP.HCM", "TH",
                "Royal Bangkok Sports Club, Bangkok", date(14), date(28), MovementType.TEMPORARY, TransportPurpose.COMPETITION,
                TransportMode.ROAD, null, List.of(rh("Morning Glory", "STANDARD", "150000")));
        requestService.submit(p("customer2"), sub.id());
        at(days(-2, 11, 0));
        uploadAll("customer2", sub.id(), date(-3), List.of(DocumentType.HEALTH_CERTIFICATE, DocumentType.LAB_TEST_REPORT,
                DocumentType.CUSTOMS_DECLARATION, DocumentType.QUARANTINE_CERTIFICATE));

        at(days(-6, 9, 0));
        RequestDetail info = createRequest("customer2", 166, "Kho ngựa Phương Nam, Bình Chánh, TP.HCM", "SG",
                "Singapore Turf Club, Kranji", date(20), date(40), MovementType.TEMPORARY, TransportPurpose.RACING,
                TransportMode.ROAD_AIR, null, List.of(rh("Lucky Comet", "STANDARD", "120000")));
        requestService.submit(p("customer2"), info.id());
        at(days(-5, 9, 0));
        List<DocumentResponse> docs = uploadAll("customer2", info.id(), date(-6), List.of(DocumentType.HEALTH_CERTIFICATE,
                DocumentType.LAB_TEST_REPORT, DocumentType.QUARANTINE_CERTIFICATE, DocumentType.CUSTOMS_DECLARATION,
                DocumentType.IMPORT_PERMIT, DocumentType.EXPORT_PERMIT));
        at(days(-4, 10, 0));
        UserPrincipal sp = p("specialist");
        requestService.startDocumentReview(sp, info.id());
        Consumer<DocumentResponse> review = d -> documentService.review(sp, d.id(),
                d.documentType() == DocumentType.VACCINATION_RECORD
                        ? new ReviewRequest(DocumentReviewStatus.NEEDS_UPDATE, "Thiếu trang xác nhận mũi tiêm nhắc lại gần nhất")
                        : new ReviewRequest(DocumentReviewStatus.APPROVED, "Hợp lệ"));
        docs.forEach(review);
        requestService.requestInformation(sp, info.id(), new DecisionRequest(
                "Vui lòng bổ sung: hồ sơ tiêm phòng đầy đủ, giấy phép nhập khẩu Singapore và giấy phép xuất khẩu."));
    }
}
