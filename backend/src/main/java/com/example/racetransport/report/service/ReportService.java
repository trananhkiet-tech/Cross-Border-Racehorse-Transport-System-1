package com.example.racetransport.report.service;

import com.example.racetransport.claim.entity.Claim;
import com.example.racetransport.claim.entity.ClaimStatus;
import com.example.racetransport.claim.entity.ClaimType;
import com.example.racetransport.claim.entity.SettlementStatus;
import com.example.racetransport.claim.repository.ClaimRepository;
import com.example.racetransport.claim.repository.ClaimSettlementRepository;
import com.example.racetransport.common.util.Money;
import com.example.racetransport.document.entity.Document;
import com.example.racetransport.document.entity.DocumentReviewStatus;
import com.example.racetransport.document.repository.DocumentRepository;
import com.example.racetransport.fleet.entity.Vehicle;
import com.example.racetransport.fleet.entity.VehicleStatus;
import com.example.racetransport.fleet.repository.VehicleRepository;
import com.example.racetransport.incident.entity.Incident;
import com.example.racetransport.incident.entity.IncidentSeverity;
import com.example.racetransport.incident.entity.IncidentStatus;
import com.example.racetransport.incident.entity.IncidentType;
import com.example.racetransport.incident.entity.OperationalDecision;
import com.example.racetransport.incident.repository.IncidentRepository;
import com.example.racetransport.payment.entity.Invoice;
import com.example.racetransport.payment.entity.InvoiceStatus;
import com.example.racetransport.payment.entity.Payment;
import com.example.racetransport.payment.entity.PaymentStatus;
import com.example.racetransport.payment.repository.InvoiceRepository;
import com.example.racetransport.payment.repository.PaymentRepository;
import com.example.racetransport.quotation.entity.CostCategory;
import com.example.racetransport.quotation.entity.Quote;
import com.example.racetransport.quotation.entity.QuoteStatus;
import com.example.racetransport.quotation.entity.QuoteType;
import com.example.racetransport.quotation.repository.QuoteItemRepository;
import com.example.racetransport.quotation.repository.QuoteRepository;
import com.example.racetransport.report.dto.ReportDtos.ClaimReport;
import com.example.racetransport.report.dto.ReportDtos.IncidentReport;
import com.example.racetransport.report.dto.ReportDtos.Kpi;
import com.example.racetransport.report.dto.ReportDtos.KpiReport;
import com.example.racetransport.report.dto.ReportDtos.Point;
import com.example.racetransport.report.dto.ReportDtos.RevenueReport;
import com.example.racetransport.report.dto.ReportDtos.TransportReport;
import com.example.racetransport.tracking.entity.TrackingEventType;
import com.example.racetransport.tracking.repository.TrackingEventRepository;
import com.example.racetransport.tracking.service.TripStateMachine;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportMode;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.transportationrequest.repository.TransportationRequestRepository;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Báo cáo cho Logistics Manager. Tính toán trực tiếp từ dữ liệu database (quy mô đồ án);
 * khi chạy profile dev, số liệu là DỮ LIỆU DEMO, không phải thống kê thật.
 */
@Service
public class ReportService {

    public static final String DEMO_NOTE = "Số liệu tính từ database hiện tại; ở môi trường dev đây là dữ liệu demo.";
    private static final int MONTHS = 6;

    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final QuoteRepository quotes;
    private final QuoteItemRepository quoteItems;
    private final TransportationTripRepository trips;
    private final TransportationRequestRepository requests;
    private final RequestHorseRepository requestHorses;
    private final IncidentRepository incidents;
    private final ClaimRepository claims;
    private final ClaimSettlementRepository settlements;
    private final DocumentRepository documents;
    private final VehicleRepository vehicles;
    private final TrackingEventRepository trackingEvents;
    private final Clock clock;

    public ReportService(InvoiceRepository invoices, PaymentRepository payments, QuoteRepository quotes,
                         QuoteItemRepository quoteItems, TransportationTripRepository trips,
                         TransportationRequestRepository requests, RequestHorseRepository requestHorses,
                         IncidentRepository incidents, ClaimRepository claims, ClaimSettlementRepository settlements,
                         DocumentRepository documents, VehicleRepository vehicles,
                         TrackingEventRepository trackingEvents, Clock clock) {
        this.invoices = invoices;
        this.payments = payments;
        this.quotes = quotes;
        this.quoteItems = quoteItems;
        this.trips = trips;
        this.requests = requests;
        this.requestHorses = requestHorses;
        this.incidents = incidents;
        this.claims = claims;
        this.settlements = settlements;
        this.documents = documents;
        this.vehicles = vehicles;
        this.trackingEvents = trackingEvents;
        this.clock = clock;
    }

    // ================================================================== revenue
    @Transactional(readOnly = true)
    public RevenueReport revenue() {
        List<Invoice> all = invoices.findAll().stream().filter(i -> i.getStatus() != InvoiceStatus.CANCELLED).toList();
        BigDecimal invoiced = sum(all.stream().map(Invoice::getTotalAmount).toList());
        BigDecimal collected = sum(all.stream().map(Invoice::getPaidAmount).toList());
        Map<YearMonth, BigDecimal[]> monthly = emptyMonths();
        for (Invoice i : all) {
            BigDecimal[] slot = monthly.get(YearMonth.from(i.getIssueDate()));
            if (slot != null) {
                slot[0] = slot[0].add(i.getTotalAmount());
            }
        }
        for (Payment p : payments.findAll()) {
            if (p.getStatus() == PaymentStatus.SUCCEEDED && p.getPaidAt() != null) {
                BigDecimal[] slot = monthly.get(YearMonth.from(p.getPaidAt()));
                if (slot != null) {
                    slot[1] = slot[1].add(p.getAmount());
                }
            }
        }
        List<Point> monthPoints = monthly.entrySet().stream()
                .map(e -> Point.of(label(e.getKey()), Money.of(e.getValue()[0]), Money.of(e.getValue()[1]))).toList();
        Map<CostCategory, BigDecimal> byCat = new EnumMap<>(CostCategory.class);
        for (Object[] row : quoteItems.sumAcceptedByCategory()) {
            byCat.put((CostCategory) row[0], Money.of((BigDecimal) row[1]));
        }
        List<Point> breakdown = byCat.entrySet().stream().map(e -> Point.of(e.getKey().getLabel(), e.getValue())).toList();
        BigDecimal acceptedQuotes = sum(quotes.findAll().stream()
                .filter(q -> q.getQuoteType() == QuoteType.FINAL && q.getStatus() == QuoteStatus.ACCEPTED)
                .map(Quote::getTotalAmount).toList());
        BigDecimal settledClaims = sum(settlements.findAll().stream().filter(s -> s.getStatus() == SettlementStatus.PAID)
                .map(s -> s.getAmount()).toList());
        return new RevenueReport("USD", invoiced, collected, Money.of(invoiced.subtract(collected)), acceptedQuotes,
                all.size(), all.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).count(), monthPoints, breakdown,
                settledClaims, DEMO_NOTE);
    }

    // ================================================================== transport
    @Transactional(readOnly = true)
    public TransportReport transport() {
        List<TransportationTrip> all = trips.findAll();
        Map<TripStatus, Long> byStatus = all.stream().collect(Collectors.groupingBy(TransportationTrip::getStatus,
                () -> new EnumMap<>(TripStatus.class), Collectors.counting()));
        Map<YearMonth, BigDecimal[]> months = emptyMonths();
        all.forEach(t -> {
            BigDecimal[] slot = months.get(YearMonth.from(t.getPlannedDeparture()));
            if (slot != null) {
                slot[0] = slot[0].add(BigDecimal.ONE);
                if (t.getStatus() == TripStatus.DELIVERED) {
                    slot[1] = slot[1].add(BigDecimal.ONE);
                }
            }
        });
        List<TransportationRequest> reqs = requests.findAll();
        Map<RequestStatus, Long> reqByStatus = reqs.stream().collect(Collectors.groupingBy(TransportationRequest::getStatus,
                () -> new EnumMap<>(RequestStatus.class), Collectors.counting()));
        Map<String, Long> routesCount = all.stream().filter(t -> t.getStatus() != TripStatus.CANCELLED)
                .collect(Collectors.groupingBy(t -> t.getRoute().getName(), Collectors.counting()));
        List<Point> topRoutes = routesCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5)
                .map(e -> Point.of(e.getKey(), e.getValue())).toList();
        Map<TransportMode, Long> modes = reqs.stream().filter(r -> r.getStatus() != RequestStatus.DRAFT)
                .collect(Collectors.groupingBy(TransportationRequest::getTransportMode, () -> new EnumMap<>(TransportMode.class),
                        Collectors.counting()));
        long horses = all.stream().filter(t -> t.getStatus() == TripStatus.DELIVERED)
                .mapToLong(t -> requestHorses.countByRequestId(t.getRequest().getId())).sum();
        return new TransportReport(all.size(), all.stream().filter(t -> TripStateMachine.ACTIVE.contains(t.getStatus())).count(),
                byStatus.getOrDefault(TripStatus.DELIVERED, 0L), byStatus.getOrDefault(TripStatus.CANCELLED, 0L), horses,
                byStatus.entrySet().stream().map(e -> Point.of(e.getKey().getLabel(), e.getValue())).toList(),
                months.entrySet().stream().map(e -> Point.of(label(e.getKey()), e.getValue()[0], e.getValue()[1])).toList(),
                reqByStatus.entrySet().stream().map(e -> Point.of(e.getKey().getLabel(), e.getValue())).toList(),
                topRoutes, modes.entrySet().stream().map(e -> Point.of(e.getKey().getLabel(), e.getValue())).toList());
    }

    // ================================================================== incidents
    @Transactional(readOnly = true)
    public IncidentReport incidents() {
        List<Incident> all = incidents.findAll();
        Map<IncidentSeverity, Long> sev = all.stream().collect(Collectors.groupingBy(Incident::getSeverity,
                () -> new EnumMap<>(IncidentSeverity.class), Collectors.counting()));
        Map<IncidentType, Long> type = all.stream().collect(Collectors.groupingBy(Incident::getIncidentType,
                () -> new EnumMap<>(IncidentType.class), Collectors.counting()));
        Map<OperationalDecision, Long> dec = all.stream().filter(i -> i.getDecision() != null)
                .collect(Collectors.groupingBy(Incident::getDecision, () -> new EnumMap<>(OperationalDecision.class), Collectors.counting()));
        Map<YearMonth, BigDecimal[]> months = emptyMonths();
        all.forEach(i -> {
            BigDecimal[] slot = months.get(YearMonth.from(i.getReportedAt()));
            if (slot != null) {
                slot[0] = slot[0].add(BigDecimal.ONE);
                if (i.getSeverity() == IncidentSeverity.HIGH || i.getSeverity() == IncidentSeverity.CRITICAL) {
                    slot[1] = slot[1].add(BigDecimal.ONE);
                }
            }
        });
        List<Incident> resolved = all.stream().filter(i -> i.getResolvedAt() != null).toList();
        BigDecimal avgHours = resolved.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(resolved.stream()
                .mapToLong(i -> Duration.between(i.getReportedAt(), i.getResolvedAt()).toMinutes()).average().orElse(0) / 60.0)
                .setScale(1, RoundingMode.HALF_UP);
        long open = all.stream().filter(i -> i.getStatus() != IncidentStatus.RESOLVED && i.getStatus() != IncidentStatus.CLOSED).count();
        return new IncidentReport(all.size(), open, sev.getOrDefault(IncidentSeverity.CRITICAL, 0L), avgHours,
                sev.entrySet().stream().map(e -> Point.of(e.getKey().getLabel(), e.getValue())).toList(),
                type.entrySet().stream().map(e -> Point.of(e.getKey().getLabel(), e.getValue())).toList(),
                months.entrySet().stream().map(e -> Point.of(label(e.getKey()), e.getValue()[0], e.getValue()[1])).toList(),
                dec.entrySet().stream().map(e -> Point.of(e.getKey().getLabel(), e.getValue())).toList(),
                trackingEvents.sumAllDelay(TrackingEventType.DELAY));
    }

    // ================================================================== claims
    @Transactional(readOnly = true)
    public ClaimReport claims() {
        List<Claim> all = claims.findAll();
        Map<ClaimStatus, Long> st = all.stream().collect(Collectors.groupingBy(Claim::getStatus,
                () -> new EnumMap<>(ClaimStatus.class), Collectors.counting()));
        Map<ClaimType, Long> ty = all.stream().collect(Collectors.groupingBy(Claim::getClaimType,
                () -> new EnumMap<>(ClaimType.class), Collectors.counting()));
        BigDecimal claimed = sum(all.stream().map(Claim::getClaimedAmount).toList());
        BigDecimal approved = sum(all.stream().map(Claim::getApprovedAmount).toList());
        BigDecimal decidedClaimed = sum(all.stream().filter(c -> c.getDecidedAt() != null).map(Claim::getClaimedAmount).toList());
        BigDecimal settled = sum(settlements.findAll().stream().filter(s -> s.getStatus() == SettlementStatus.PAID)
                .map(s -> s.getAmount()).toList());
        BigDecimal ratio = decidedClaimed.signum() == 0 ? BigDecimal.ZERO
                : approved.multiply(BigDecimal.valueOf(100)).divide(decidedClaimed, 1, RoundingMode.HALF_UP);
        long open = all.stream().filter(c -> c.getStatus() == ClaimStatus.SUBMITTED || c.getStatus() == ClaimStatus.UNDER_REVIEW
                || c.getStatus() == ClaimStatus.EVIDENCE_REQUIRED).count();
        return new ClaimReport(all.size(), open, claimed, approved, settled, ratio,
                st.entrySet().stream().map(e -> Point.of(e.getKey().getLabel(), e.getValue())).toList(),
                ty.entrySet().stream().map(e -> Point.of(e.getKey().getLabel(), e.getValue())).toList());
    }

    // ================================================================== KPI
    @Transactional(readOnly = true)
    public KpiReport kpi() {
        List<Kpi> list = new ArrayList<>();
        List<TransportationTrip> delivered = trips.findByStatusIn(List.of(TripStatus.DELIVERED));
        long onTime = delivered.stream().filter(t -> t.getActualArrival() != null
                && !t.getActualArrival().isAfter(t.getPlannedArrival().plusMinutes(60))).count();
        list.add(new Kpi("ON_TIME_RATE", "Tỷ lệ giao đúng hạn", percent(onTime, delivered.size()), "%",
                "Chuyến đã giao có giờ đến thực tế ≤ giờ đến dự kiến + 60 phút"));
        long totalTrips = trips.count();
        long delay = trackingEvents.sumAllDelay(TrackingEventType.DELAY);
        list.add(new Kpi("AVG_DELAY", "Trễ trung bình mỗi chuyến", totalTrips == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(delay).divide(BigDecimal.valueOf(totalTrips), 1, RoundingMode.HALF_UP), "phút",
                "Tổng phút trễ được báo / số chuyến"));
        long incidentCount = incidents.count();
        list.add(new Kpi("INCIDENT_RATE", "Sự cố trên 10 chuyến", totalTrips == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(incidentCount * 10.0 / totalTrips).setScale(1, RoundingMode.HALF_UP), "sự cố",
                "Số sự cố / số chuyến × 10"));
        List<Document> docs = documents.findAll();
        long docApproved = docs.stream().filter(d -> d.getReviewStatus() == DocumentReviewStatus.APPROVED).count();
        long docReviewed = docs.stream().filter(d -> d.getReviewStatus() != DocumentReviewStatus.PENDING_REVIEW).count();
        list.add(new Kpi("DOC_APPROVAL", "Tỷ lệ tài liệu đạt", percent(docApproved, docReviewed), "%",
                "Tài liệu được duyệt / tài liệu đã rà soát"));
        List<Quote> finals = quotes.findAll().stream().filter(q -> q.getQuoteType() == QuoteType.FINAL).toList();
        long acc = finals.stream().filter(q -> q.getStatus() == QuoteStatus.ACCEPTED).count();
        long rej = finals.stream().filter(q -> q.getStatus() == QuoteStatus.REJECTED || q.getStatus() == QuoteStatus.EXPIRED).count();
        list.add(new Kpi("QUOTE_ACCEPTANCE", "Tỷ lệ chấp nhận báo giá", percent(acc, acc + rej), "%",
                "Báo giá cuối được chấp nhận / (chấp nhận + từ chối + hết hạn)"));
        List<TransportationRequest> approvedReqs = requests.findAll().stream()
                .filter(r -> r.getSubmittedAt() != null && r.getApprovedAt() != null).toList();
        double avgDays = approvedReqs.stream().mapToDouble(r -> Duration.between(r.getSubmittedAt(), r.getApprovedAt()).toHours() / 24.0)
                .average().orElse(0);
        list.add(new Kpi("APPROVAL_TIME", "Thời gian xét duyệt trung bình", BigDecimal.valueOf(avgDays).setScale(1, RoundingMode.HALF_UP),
                "ngày", "Từ lúc gửi yêu cầu đến khi Manager phê duyệt"));
        List<Vehicle> fleet = vehicles.findAll().stream().filter(v -> !Boolean.TRUE.equals(v.getArchived())).toList();
        list.add(new Kpi("FLEET_UTILIZATION", "Xe đang sử dụng", percent(fleet.stream()
                .filter(v -> v.getStatus() == VehicleStatus.IN_USE).count(), fleet.size()), "%", "Xe đang chạy chuyến / tổng xe"));
        ClaimReport cr = claims();
        list.add(new Kpi("CLAIM_RATIO", "Tỷ lệ số tiền bồi thường được duyệt", cr.approvalRatioPercent(), "%",
                "Tổng duyệt / tổng yêu cầu của hồ sơ đã quyết định"));
        return new KpiReport(list, DEMO_NOTE);
    }

    // ================================================================== helpers
    private Map<YearMonth, BigDecimal[]> emptyMonths() {
        Map<YearMonth, BigDecimal[]> m = new LinkedHashMap<>();
        YearMonth now = YearMonth.from(LocalDate.now(clock));
        for (int i = MONTHS - 1; i >= 0; i--) {
            m.put(now.minusMonths(i), new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
        }
        return m;
    }

    private static String label(YearMonth ym) {
        return String.format("%02d/%d", ym.getMonthValue(), ym.getYear());
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return Money.of(values.stream().filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal percent(long part, long total) {
        return total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(part * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
    }
}
