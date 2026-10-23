package com.example.racetransport.report.service;

import com.example.racetransport.claim.entity.ClaimStatus;
import com.example.racetransport.claim.repository.ClaimRepository;
import com.example.racetransport.compliance.repository.ComplianceChecklistItemRepository;
import com.example.racetransport.compliance.service.ComplianceService;
import com.example.racetransport.document.entity.DocumentReviewStatus;
import com.example.racetransport.document.repository.DocumentRepository;
import com.example.racetransport.fleet.entity.VehicleStatus;
import com.example.racetransport.fleet.repository.VehicleRepository;
import com.example.racetransport.incident.entity.IncidentSeverity;
import com.example.racetransport.incident.entity.IncidentStatus;
import com.example.racetransport.incident.repository.IncidentRepository;
import com.example.racetransport.quotation.repository.QuoteRepository;
import com.example.racetransport.report.dto.ReportDtos.Card;
import com.example.racetransport.report.dto.ReportDtos.DashboardResponse;
import com.example.racetransport.report.dto.ReportDtos.Item;
import com.example.racetransport.report.dto.ReportDtos.RevenueReport;
import com.example.racetransport.report.dto.ReportDtos.Section;
import com.example.racetransport.route.entity.RouteStatus;
import com.example.racetransport.route.entity.RouteStop;
import com.example.racetransport.route.repository.RouteRepository;
import com.example.racetransport.route.repository.RouteStopRepository;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.tracking.service.TripStateMachine;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.repository.TransportationRequestRepository;
import com.example.racetransport.transportationrequest.service.RequestWorkflow;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dashboard theo từng role – mọi con số đọc từ database (không hard-code KPI). */
@Service
public class DashboardService {

    private static final Set<ClaimStatus> CLAIM_OPEN = EnumSet.of(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW,
            ClaimStatus.EVIDENCE_REQUIRED, ClaimStatus.APPROVED, ClaimStatus.PARTIALLY_APPROVED);
    private static final Set<IncidentStatus> INCIDENT_OPEN = EnumSet.of(IncidentStatus.OPEN, IncidentStatus.UNDER_REVIEW,
            IncidentStatus.ACTION_REQUIRED, IncidentStatus.MONITORING);

    private final TransportationRequestRepository requests;
    private final TransportationTripRepository trips;
    private final QuoteRepository quotes;
    private final ClaimRepository claims;
    private final IncidentRepository incidents;
    private final DocumentRepository documents;
    private final ComplianceChecklistItemRepository checklistItems;
    private final VehicleRepository vehicles;
    private final RouteRepository routes;
    private final RouteStopRepository routeStops;
    private final ReportService reports;
    private final Clock clock;

    public DashboardService(TransportationRequestRepository requests, TransportationTripRepository trips,
                            QuoteRepository quotes, ClaimRepository claims, IncidentRepository incidents,
                            DocumentRepository documents, ComplianceChecklistItemRepository checklistItems,
                            VehicleRepository vehicles, RouteRepository routes, RouteStopRepository routeStops,
                            ReportService reports, Clock clock) {
        this.requests = requests;
        this.trips = trips;
        this.quotes = quotes;
        this.claims = claims;
        this.incidents = incidents;
        this.documents = documents;
        this.checklistItems = checklistItems;
        this.vehicles = vehicles;
        this.routes = routes;
        this.routeStops = routeStops;
        this.reports = reports;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse forUser(UserPrincipal actor) {
        return switch (actor.getRole()) {
            case CUSTOMER -> customer(actor);
            case LOGISTICS_MANAGER -> manager(actor);
            case TRANSPORT_SPECIALIST -> specialist(actor);
            case FLEET_ROUTE_COORDINATOR -> coordinator(actor);
            case DRIVER_ESCORT -> driver(actor);
        };
    }

    // ------------------------------------------------------------------ customer
    private DashboardResponse customer(UserPrincipal a) {
        Long me = a.getId();
        long active = requests.countByCustomerIdAndStatusIn(me, RequestWorkflow.ACTIVE);
        long pendingDocs = checklistItems.findOpenItems(ComplianceService.OPEN_STATUSES, me).size();
        long awaitingQuote = quotes.countAwaitingCustomer(me);
        long activeTrips = trips.countByRequestCustomerIdAndStatusIn(me, TripStateMachine.ACTIVE);
        long openClaims = claims.countByCustomerIdAndStatusIn(me, CLAIM_OPEN);
        List<Card> cards = List.of(
                new Card("activeRequests", "Yêu cầu đang xử lý", String.valueOf(active), "Không gồm nháp/đã đóng", "primary", "/requests"),
                new Card("pendingDocuments", "Tài liệu cần bổ sung", String.valueOf(pendingDocs), "Theo checklist tuân thủ",
                        pendingDocs > 0 ? "warning" : "success", "/compliance/missing"),
                new Card("quotesAwaiting", "Báo giá chờ phản hồi", String.valueOf(awaitingQuote), null,
                        awaitingQuote > 0 ? "warning" : "neutral", "/quotes"),
                new Card("activeTrips", "Chuyến đang hoạt động", String.valueOf(activeTrips), null, "info", "/trips"),
                new Card("openClaims", "Hồ sơ bồi thường mở", String.valueOf(openClaims), null, "neutral", "/claims"));
        List<Item> recent = requests.findByCustomerIdOrderByCreatedAtDesc(me).stream().limit(5)
                .map(r -> new Item(r.getId(), r.getRequestNumber(), r.getOriginAddress() + " → " + r.getDestinationAddress(),
                        r.getStatus().name(), "Khởi hành " + r.getRequestedDepartureDate(), "/requests/" + r.getId()))
                .toList();
        List<Item> myTrips = trips.search(TripStateMachine.ACTIVE, me, null, null,
                        PageRequest.of(0, 5, Sort.by("plannedDeparture"))).getContent().stream()
                .map(t -> new Item(t.getId(), t.getTripNumber(), t.getRoute().getName(), t.getStatus().name(),
                        t.getCurrentLocation(), "/trips/" + t.getId()))
                .toList();
        return new DashboardResponse(a.getRole().name(), "Xin chào, " + a.getFullName(), cards, List.of(
                new Section("recentRequests", "Yêu cầu gần đây", recent, "Bạn chưa có yêu cầu vận chuyển nào", "/requests"),
                new Section("activeTrips", "Chuyến đang theo dõi", myTrips, "Không có chuyến đang hoạt động", "/trips")),
                List.of(), null);
    }

    // ------------------------------------------------------------------ manager
    private DashboardResponse manager(UserPrincipal a) {
        long awaiting = requests.countByStatus(RequestStatus.PENDING_MANAGER_APPROVAL);
        long toQuote = requests.countByStatus(RequestStatus.APPROVED) + requests.countByStatus(RequestStatus.QUOTE_REJECTED);
        long enRoute = trips.countByStatusIn(TripStateMachine.EN_ROUTE);
        long delayed = trips.countByStatus(TripStatus.DELAYED);
        long openIncidents = incidents.countByStatusIn(INCIDENT_OPEN);
        long severe = incidents.countByStatusInAndSeverityIn(INCIDENT_OPEN, EnumSet.of(IncidentSeverity.HIGH, IncidentSeverity.CRITICAL));
        long claimsReview = claims.countByStatusIn(EnumSet.of(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, ClaimStatus.EVIDENCE_REQUIRED));
        RevenueReport rev = reports.revenue();
        List<Card> cards = List.of(
                new Card("awaitingApproval", "Chờ phê duyệt", String.valueOf(awaiting), "Hồ sơ tuân thủ đã đủ", awaiting > 0 ? "warning" : "neutral",
                        "/requests?status=PENDING_MANAGER_APPROVAL"),
                new Card("toQuote", "Cần lập báo giá cuối", String.valueOf(toQuote), null, "primary", "/quotes"),
                new Card("activeTrips", "Chuyến đang chạy", String.valueOf(enRoute), delayed + " chuyến bị trễ/tạm dừng",
                        delayed > 0 ? "warning" : "info", "/trips"),
                new Card("openIncidents", "Sự cố đang mở", String.valueOf(openIncidents), severe + " mức Cao/Nghiêm trọng",
                        severe > 0 ? "error" : "neutral", "/incidents"),
                new Card("claimsReview", "Bồi thường cần thẩm định", String.valueOf(claimsReview), null, "neutral", "/claims"),
                new Card("collected", "Đã thu (6 tháng)", rev.collectedTotal().toPlainString() + " " + rev.currency(),
                        "Còn phải thu " + rev.outstandingTotal().toPlainString(), "success", "/reports"));
        List<Item> pending = requests.findByStatusInOrderByRequestedDepartureDateAsc(EnumSet.of(RequestStatus.PENDING_MANAGER_APPROVAL))
                .stream().limit(6).map(r -> new Item(r.getId(), r.getRequestNumber(), r.getCustomer().getFullName(),
                        r.getStatus().name(), "Khởi hành " + r.getRequestedDepartureDate(), "/requests/" + r.getId()))
                .toList();
        List<Item> incidentItems = incidents.search(INCIDENT_OPEN, EnumSet.allOf(IncidentSeverity.class), null, null, null,
                        PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "reportedAt"))).getContent().stream()
                .map(i -> new Item(i.getId(), i.getIncidentNumber() + " · " + i.getTitle(), i.getTrip().getTripNumber(),
                        i.getSeverity().name(), i.getStatus().getLabel(), "/incidents/" + i.getId()))
                .toList();
        List<Item> running = trips.search(TripStateMachine.EN_ROUTE, null, null, null,
                        PageRequest.of(0, 6, Sort.by("plannedArrival"))).getContent().stream()
                .map(t -> new Item(t.getId(), t.getTripNumber(), t.getRoute().getName(), t.getStatus().name(),
                        t.getCurrentLocation(), "/trips/" + t.getId()))
                .toList();
        return new DashboardResponse(a.getRole().name(), "Tổng quan vận hành", cards, List.of(
                new Section("pendingApproval", "Yêu cầu chờ phê duyệt", pending, "Không có yêu cầu chờ phê duyệt", "/requests"),
                new Section("incidents", "Sự cố cần theo dõi", incidentItems, "Không có sự cố đang mở", "/incidents"),
                new Section("runningTrips", "Chuyến đang chạy", running, "Không có chuyến đang chạy", "/trips")),
                rev.monthly(), "Doanh thu hóa đơn / đã thu theo tháng (USD)");
    }

    // ------------------------------------------------------------------ specialist
    private DashboardResponse specialist(UserPrincipal a) {
        long pendingDocs = documents.countByReviewStatus(DocumentReviewStatus.PENDING_REVIEW);
        long missing = checklistItems.countByStatusIn(ComplianceService.OPEN_STATUSES);
        LocalDate today = LocalDate.now(clock);
        var expiring = documents.findExpiring(today, today.plusDays(30));
        long inReview = requests.countByStatusIn(EnumSet.of(RequestStatus.SUBMITTED, RequestStatus.UNDER_DOCUMENT_REVIEW,
                RequestStatus.COMPLIANCE_REVIEW));
        long incomplete = checklistItems.countRequestsWithItemStatus(ComplianceService.OPEN_STATUSES);
        List<Card> cards = List.of(
                new Card("pendingDocuments", "Tài liệu chờ rà soát", String.valueOf(pendingDocs), null,
                        pendingDocs > 0 ? "warning" : "success", "/documents?status=PENDING_REVIEW"),
                new Card("missingDocuments", "Mục hồ sơ còn thiếu", String.valueOf(missing), incomplete + " yêu cầu chưa đủ hồ sơ",
                        missing > 0 ? "warning" : "success", "/compliance/missing"),
                new Card("expiring", "Sắp hết hạn (30 ngày)", String.valueOf(expiring.size()), null,
                        expiring.isEmpty() ? "neutral" : "error", "/documents?expiring=1"),
                new Card("inReview", "Yêu cầu đang xét hồ sơ", String.valueOf(inReview), null, "primary", "/requests"));
        List<Item> docs = documents.search(EnumSet.of(DocumentReviewStatus.PENDING_REVIEW),
                        EnumSet.allOf(com.example.racetransport.document.entity.DocumentType.class), null, null,
                        PageRequest.of(0, 6, Sort.by("createdAt"))).getContent().stream()
                .map(d -> new Item(d.getId(), d.getDocumentType().getLabel(),
                        (d.getRequest() == null ? "" : d.getRequest().getRequestNumber() + " · ")
                                + (d.getHorse() == null ? "" : d.getHorse().getRegisteredName()),
                        d.getReviewStatus().name(), d.getFile().getOriginalFilename(), "/documents/" + d.getId()))
                .toList();
        List<Item> reqs = requests.findByStatusInOrderByRequestedDepartureDateAsc(EnumSet.of(RequestStatus.SUBMITTED,
                        RequestStatus.UNDER_DOCUMENT_REVIEW, RequestStatus.COMPLIANCE_REVIEW)).stream().limit(6)
                .map(r -> new Item(r.getId(), r.getRequestNumber(), r.getCustomer().getFullName(), r.getStatus().name(),
                        "Khởi hành " + r.getRequestedDepartureDate(), "/requests/" + r.getId() + "/compliance"))
                .toList();
        List<Item> exp = expiring.stream().limit(6).map(d -> new Item(d.getId(), d.getDocumentType().getLabel(),
                d.getHorse() == null ? "" : d.getHorse().getRegisteredName(), d.getReviewStatus().name(),
                "Hết hạn " + d.getExpiryDate(), "/documents/" + d.getId())).toList();
        return new DashboardResponse(a.getRole().name(), "Rà soát hồ sơ & tuân thủ", cards, List.of(
                new Section("pendingDocuments", "Tài liệu chờ rà soát (cũ nhất trước)", docs, "Không có tài liệu chờ", "/documents"),
                new Section("requestsInReview", "Yêu cầu đang xét hồ sơ", reqs, "Không có yêu cầu", "/requests"),
                new Section("expiring", "Tài liệu sắp hết hạn", exp, "Không có tài liệu sắp hết hạn", "/documents")),
                List.of(), null);
    }

    // ------------------------------------------------------------------ coordinator
    private DashboardResponse coordinator(UserPrincipal a) {
        long available = vehicles.countByStatusAndArchivedFalse(VehicleStatus.AVAILABLE);
        long maintenance = vehicles.countByStatusAndArchivedFalse(VehicleStatus.MAINTENANCE);
        LocalDateTime now = LocalDateTime.now(clock);
        var upcoming = trips.findUpcoming(TripStateMachine.PRE_DEPARTURE, now.toLocalDate().atStartOfDay(),
                now.toLocalDate().plusDays(8).atTime(LocalTime.MIDNIGHT));
        var needing = trips.findNeedingAssignment(TripStateMachine.PRE_DEPARTURE);
        long ready = requests.countByStatus(RequestStatus.READY_FOR_PLANNING);
        long activeRoutes = routes.countByStatus(RouteStatus.ACTIVE);
        List<Card> cards = List.of(
                new Card("readyForPlanning", "Yêu cầu chờ lập chuyến", String.valueOf(ready), null, ready > 0 ? "warning" : "neutral", "/planning"),
                new Card("needAssignment", "Chuyến thiếu xe/tài xế", String.valueOf(needing.size()), null,
                        needing.isEmpty() ? "success" : "error", "/planning"),
                new Card("upcoming", "Chuyến 7 ngày tới", String.valueOf(upcoming.size()), null, "info", "/trips"),
                new Card("availableVehicles", "Xe sẵn sàng", String.valueOf(available), maintenance + " xe đang bảo dưỡng", "success", "/fleet"),
                new Card("activeRoutes", "Tuyến đang dùng", String.valueOf(activeRoutes), null, "primary", "/routes"));
        List<Item> readyItems = requests.findByStatusInOrderByRequestedDepartureDateAsc(EnumSet.of(RequestStatus.READY_FOR_PLANNING))
                .stream().limit(6).map(r -> new Item(r.getId(), r.getRequestNumber(),
                        r.getOriginAddress() + " → " + r.getDestinationAddress(), r.getStatus().name(),
                        "Khởi hành " + r.getRequestedDepartureDate(), "/planning?requestId=" + r.getId()))
                .toList();
        List<Item> needItems = needing.stream().limit(6).map(t -> tripItem(t, t.getVehicle() == null ? "Chưa có xe" : "Chưa có tài xế")).toList();
        List<Item> upItems = upcoming.stream().limit(6).map(t -> tripItem(t, "Khởi hành " + t.getPlannedDeparture())).toList();
        return new DashboardResponse(a.getRole().name(), "Điều phối đội xe & tuyến", cards, List.of(
                new Section("readyForPlanning", "Yêu cầu sẵn sàng lập chuyến", readyItems, "Không có yêu cầu chờ lập chuyến", "/planning"),
                new Section("needAssignment", "Chuyến cần phân công", needItems, "Mọi chuyến đã đủ xe và tài xế", "/trips"),
                new Section("upcoming", "Chuyến sắp khởi hành", upItems, "Không có chuyến trong 7 ngày tới", "/trips")),
                List.of(), null);
    }

    // ------------------------------------------------------------------ driver/escort
    private DashboardResponse driver(UserPrincipal a) {
        var mine = trips.search(TripStateMachine.ACTIVE, null, a.getId(), null,
                PageRequest.of(0, 20, Sort.by("plannedDeparture"))).getContent();
        long pre = mine.stream().filter(t -> TripStateMachine.PRE_DEPARTURE.contains(t.getStatus())).count();
        long running = mine.stream().filter(t -> TripStateMachine.EN_ROUTE.contains(t.getStatus())).count();
        long handover = mine.stream().filter(t -> t.getStatus() == TripStatus.ARRIVED || t.getStatus() == TripStatus.HANDOVER_PENDING).count();
        var myIncidents = incidents.search(INCIDENT_OPEN, EnumSet.allOf(IncidentSeverity.class), null, null, a.getId(),
                PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "reportedAt"))).getContent();
        List<Card> cards = List.of(
                new Card("assigned", "Chuyến được giao", String.valueOf(mine.size()), null, "primary", "/trips"),
                new Card("upcoming", "Sắp khởi hành", String.valueOf(pre), "Cần kiểm tra trước vận chuyển", pre > 0 ? "warning" : "neutral", "/trips"),
                new Card("running", "Đang trên đường", String.valueOf(running), null, "info", "/trips"),
                new Card("handover", "Chờ bàn giao", String.valueOf(handover), null, handover > 0 ? "warning" : "neutral", "/trips"),
                new Card("incidents", "Sự cố đang xử lý", String.valueOf(myIncidents.size()), null,
                        myIncidents.isEmpty() ? "success" : "error", "/incidents"));
        List<Item> tripItems = mine.stream().limit(6).map(t -> tripItem(t, t.getCurrentLocation())).toList();
        List<Item> next = new ArrayList<>();
        for (TransportationTrip t : mine) {
            if (!TripStateMachine.EN_ROUTE.contains(t.getStatus()) && t.getStatus() != TripStatus.READY_FOR_DEPARTURE) {
                continue;
            }
            List<RouteStop> stops = routeStops.findByRouteIdOrderByStopOrderAsc(t.getRoute().getId());
            int current = t.getCurrentStop() == null ? 0 : t.getCurrentStop().getStopOrder();
            stops.stream().filter(s -> s.getStopOrder() > current).limit(3).forEach(s -> next.add(new Item(s.getId(),
                    s.getName(), t.getTripNumber() + " · " + s.getStopType().getLabel(), t.getStatus().name(),
                    s.getPlannedArrivalOffsetMin() == null ? null
                            : "Dự kiến " + t.getPlannedDeparture().plusMinutes(s.getPlannedArrivalOffsetMin()),
                    "/trips/" + t.getId())));
        }
        List<Item> incidentItems = myIncidents.stream().map(i -> new Item(i.getId(), i.getIncidentNumber() + " · " + i.getTitle(),
                i.getTrip().getTripNumber(), i.getSeverity().name(), i.getStatus().getLabel(), "/incidents/" + i.getId())).toList();
        return new DashboardResponse(a.getRole().name(), "Xin chào, " + a.getFullName(), cards, List.of(
                new Section("myTrips", "Chuyến của tôi", tripItems, "Bạn chưa được phân công chuyến nào", "/trips"),
                new Section("nextCheckpoints", "Điểm dừng sắp tới", next, "Không có điểm dừng sắp tới", "/trips"),
                new Section("incidents", "Sự cố đang xử lý", incidentItems, "Không có sự cố", "/incidents")),
                List.of(), null);
    }

    private static Item tripItem(TransportationTrip t, String meta) {
        return new Item(t.getId(), t.getTripNumber(), t.getRoute().getName(), t.getStatus().name(), meta, "/trips/" + t.getId());
    }
}
