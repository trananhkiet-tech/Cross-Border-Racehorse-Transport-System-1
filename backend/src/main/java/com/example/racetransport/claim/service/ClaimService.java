package com.example.racetransport.claim.service;

import static com.example.racetransport.claim.entity.ClaimStatus.*;

import com.example.racetransport.claim.dto.ClaimDtos.ApproveClaimRequest;
import com.example.racetransport.claim.dto.ClaimDtos.AssessmentResponse;
import com.example.racetransport.claim.dto.ClaimDtos.ClaimDetail;
import com.example.racetransport.claim.dto.ClaimDtos.ClaimSummary;
import com.example.racetransport.claim.dto.ClaimDtos.CreateClaimRequest;
import com.example.racetransport.claim.dto.ClaimDtos.EvidenceResponse;
import com.example.racetransport.claim.dto.ClaimDtos.ReviewClaimRequest;
import com.example.racetransport.claim.dto.ClaimDtos.SettlementRequest;
import com.example.racetransport.claim.dto.ClaimDtos.SettlementResponse;
import com.example.racetransport.claim.entity.Claim;
import com.example.racetransport.claim.entity.ClaimAssessment;
import com.example.racetransport.claim.entity.ClaimEvidence;
import com.example.racetransport.claim.entity.ClaimRecommendation;
import com.example.racetransport.claim.entity.ClaimSettlement;
import com.example.racetransport.claim.entity.ClaimStatus;
import com.example.racetransport.claim.entity.ClaimType;
import com.example.racetransport.claim.entity.EvidenceType;
import com.example.racetransport.claim.entity.SettlementStatus;
import com.example.racetransport.claim.repository.ClaimAssessmentRepository;
import com.example.racetransport.claim.repository.ClaimEvidenceRepository;
import com.example.racetransport.claim.repository.ClaimRepository;
import com.example.racetransport.claim.repository.ClaimSettlementRepository;
import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.common.util.Money;
import com.example.racetransport.document.entity.StoredFile;
import com.example.racetransport.document.service.FileStorageService;
import com.example.racetransport.incident.entity.Incident;
import com.example.racetransport.incident.repository.IncidentRepository;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.transportationrequest.entity.RequestHorse;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.transportationrequest.service.RequestAccess;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Claim / Evidence / Assessment / Settlement – luồng bồi thường tài chính, TÁCH BIỆT với Incident.
 * Số tiền duyệt do Manager quyết định dựa trên bằng chứng và thẩm định, không tự động bằng
 * giá trị khai báo của ngựa (giá trị khai báo chỉ là tham chiếu/mức trần với các loại tổn thất toàn bộ).
 */
@Service
public class ClaimService {

    private static final Set<ClaimType> TOTAL_LOSS = EnumSet.of(ClaimType.MORTALITY, ClaimType.HUMANE_EUTHANASIA,
            ClaimType.THEFT_OR_LOSS);
    private static final Set<ClaimStatus> REVIEWABLE = EnumSet.of(SUBMITTED, UNDER_REVIEW, EVIDENCE_REQUIRED);

    private final ClaimRepository claims;
    private final ClaimEvidenceRepository evidences;
    private final ClaimAssessmentRepository assessments;
    private final ClaimSettlementRepository settlements;
    private final IncidentRepository incidents;
    private final TransportationTripRepository trips;
    private final RequestHorseRepository requestHorses;
    private final RequestAccess requestAccess;
    private final FileStorageService storage;
    private final NotificationService notifications;
    private final AppUserRepository users;
    private final CodeGenerator codes;
    private final Clock clock;

    public ClaimService(ClaimRepository claims, ClaimEvidenceRepository evidences, ClaimAssessmentRepository assessments,
                        ClaimSettlementRepository settlements, IncidentRepository incidents,
                        TransportationTripRepository trips, RequestHorseRepository requestHorses,
                        RequestAccess requestAccess, FileStorageService storage, NotificationService notifications,
                        AppUserRepository users, CodeGenerator codes, Clock clock) {
        this.claims = claims;
        this.evidences = evidences;
        this.assessments = assessments;
        this.settlements = settlements;
        this.incidents = incidents;
        this.trips = trips;
        this.requestHorses = requestHorses;
        this.requestAccess = requestAccess;
        this.storage = storage;
        this.notifications = notifications;
        this.users = users;
        this.codes = codes;
        this.clock = clock;
    }

    // ================================================================== customer
    @Transactional
    public ClaimDetail create(UserPrincipal actor, CreateClaimRequest req) {
        TransportationRequest r = requestAccess.loadOwned(actor, req.requestId());
        Guard.state(r.getStatus() == RequestStatus.TRIP_PLANNED || r.getStatus() == RequestStatus.COMPLETED,
                "Chỉ nộp yêu cầu bồi thường cho yêu cầu đã có chuyến vận chuyển");
        TransportationTrip trip = null;
        Incident incident = null;
        if (req.incidentId() != null) {
            incident = incidents.findById(req.incidentId()).orElseThrow(() -> new NotFoundException("Không tìm thấy sự cố"));
            Guard.input(incident.getTrip().getRequest().getId().equals(r.getId()), "Sự cố không thuộc yêu cầu này");
            trip = incident.getTrip();
        }
        if (req.tripId() != null) {
            TransportationTrip t = trips.findById(req.tripId()).orElseThrow(() -> new NotFoundException("Không tìm thấy chuyến"));
            Guard.input(t.getRequest().getId().equals(r.getId()), "Chuyến không thuộc yêu cầu này");
            Guard.input(trip == null || trip.getId().equals(t.getId()), "Sự cố và chuyến không khớp");
            trip = t;
        }
        RequestHorse rh = null;
        if (req.horseId() != null) {
            rh = requestHorses.findByRequestIdFetch(r.getId()).stream()
                    .filter(x -> x.getHorse().getId().equals(req.horseId())).findFirst()
                    .orElseThrow(() -> new com.example.racetransport.common.exception.BadRequestException("Ngựa không thuộc yêu cầu này"));
        } else if (incident != null && incident.getHorse() != null) {
            Long hid = incident.getHorse().getId();
            rh = requestHorses.findByRequestIdFetch(r.getId()).stream().filter(x -> x.getHorse().getId().equals(hid))
                    .findFirst().orElse(null);
        }
        if (TOTAL_LOSS.contains(req.claimType()) || req.claimType() == ClaimType.INJURY_MEDICAL_EXPENSE) {
            Guard.input(rh != null, "Loại bồi thường này cần chọn ngựa liên quan");
        }
        if (TOTAL_LOSS.contains(req.claimType())) {
            Guard.input(req.claimedAmount().compareTo(rh.getDeclaredValue()) <= 0,
                    "Số tiền yêu cầu không vượt quá giá trị khai báo của ngựa (" + rh.getDeclaredValue().toPlainString() + ")");
        }
        Claim c = new Claim();
        c.setClaimNumber(codes.next(CodeGenerator.CLAIM));
        c.setRequest(r);
        c.setTrip(trip);
        c.setIncident(incident);
        c.setHorse(rh == null ? null : rh.getHorse());
        c.setCustomer(users.getReferenceById(actor.getId()));
        c.setClaimType(req.claimType());
        c.setClaimedAmount(Money.of(req.claimedAmount()));
        c.setCurrency(r.getCurrency());
        c.setDescription(req.description().trim());
        c.setStatus(SUBMITTED);
        c.setSubmittedAt(LocalDateTime.now(clock));
        claims.save(c);
        notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.ACTION_REQUIRED,
                "Yêu cầu bồi thường mới " + c.getClaimNumber(),
                req.claimType().getLabel() + " – " + c.getClaimedAmount().toPlainString() + " " + c.getCurrency(),
                NotificationService.REF_CLAIM, c.getId());
        return toDetail(actor, c);
    }

    @Transactional
    public ClaimDetail addEvidence(UserPrincipal actor, Long id, MultipartFile file, EvidenceType type, String notes) {
        Claim c = load(id);
        boolean owner = actor.isCustomer() && c.getCustomer().getId().equals(actor.getId());
        if (!owner && !actor.isManager()) {
            throw new ForbiddenException("Bạn không có quyền thêm bằng chứng cho hồ sơ này");
        }
        Guard.state(REVIEWABLE.contains(c.getStatus()), "Hồ sơ đã có quyết định, không thể bổ sung bằng chứng");
        Guard.input(type != null, "Cần chọn loại bằng chứng");
        AppUser user = users.getReferenceById(actor.getId());
        StoredFile f = storage.store(file, user);
        ClaimEvidence e = new ClaimEvidence();
        e.setClaim(c);
        e.setFile(f);
        e.setEvidenceType(type);
        e.setNotes(notes);
        e.setUploadedBy(user);
        e.setUploadedAt(LocalDateTime.now(clock));
        evidences.save(e);
        if (owner && c.getStatus() == EVIDENCE_REQUIRED) {
            c.setStatus(UNDER_REVIEW);
            notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.ACTION_REQUIRED,
                    "Khách hàng bổ sung bằng chứng " + c.getClaimNumber(), type.getLabel(), NotificationService.REF_CLAIM, c.getId());
        }
        return toDetail(actor, c);
    }

    // ================================================================== manager
    @Transactional
    public ClaimDetail review(UserPrincipal actor, Long id, ReviewClaimRequest req) {
        requireManager(actor);
        Claim c = load(id);
        Guard.state(REVIEWABLE.contains(c.getStatus()), "Hồ sơ không ở giai đoạn thẩm định");
        if (req.recommendedAmount() != null) {
            Guard.input(req.recommendedAmount().compareTo(c.getClaimedAmount()) <= 0, "Số đề xuất không vượt quá số yêu cầu");
        }
        AppUser manager = users.getReferenceById(actor.getId());
        ClaimAssessment a = new ClaimAssessment();
        a.setClaim(c);
        a.setAssessmentNote(req.assessmentNote().trim());
        a.setRecommendation(req.recommendation());
        a.setRecommendedAmount(req.recommendedAmount() == null ? null : Money.of(req.recommendedAmount()));
        a.setAssessedBy(manager);
        a.setAssessedAt(LocalDateTime.now(clock));
        assessments.save(a);
        c.setReviewedBy(manager);
        if (req.recommendation() == ClaimRecommendation.REQUEST_EVIDENCE) {
            c.setStatus(EVIDENCE_REQUIRED);
            notifications.notify(c.getCustomer(), NotificationType.ACTION_REQUIRED,
                    "Cần bổ sung bằng chứng cho " + c.getClaimNumber(), req.assessmentNote(), NotificationService.REF_CLAIM, c.getId());
        } else {
            c.setStatus(UNDER_REVIEW);
            if (c.getStatus() == UNDER_REVIEW && assessments.countByClaimId(c.getId()) == 1) {
                notifications.notify(c.getCustomer(), NotificationType.INFO, "Hồ sơ " + c.getClaimNumber() + " đang được thẩm định",
                        "Chúng tôi sẽ thông báo khi có quyết định.", NotificationService.REF_CLAIM, c.getId());
            }
        }
        return toDetail(actor, c);
    }

    @Transactional
    public ClaimDetail approve(UserPrincipal actor, Long id, ApproveClaimRequest req) {
        requireManager(actor);
        Claim c = load(id);
        Guard.state(c.getStatus() == UNDER_REVIEW, "Chỉ quyết định hồ sơ đang thẩm định");
        Guard.state(assessments.countByClaimId(c.getId()) > 0, "Cần ít nhất một nhận định thẩm định trước khi phê duyệt");
        Guard.state(evidences.countByClaimId(c.getId()) > 0, "Hồ sơ chưa có bằng chứng");
        BigDecimal approved = Money.of(req.approvedAmount());
        Guard.input(approved.compareTo(c.getClaimedAmount()) <= 0, "Số duyệt không vượt quá số yêu cầu");
        c.setApprovedAmount(approved);
        c.setStatus(approved.compareTo(c.getClaimedAmount()) == 0 ? APPROVED : PARTIALLY_APPROVED);
        c.setDecisionNote(req.decisionNote().trim());
        c.setReviewedBy(users.getReferenceById(actor.getId()));
        c.setDecidedAt(LocalDateTime.now(clock));
        notifications.notify(c.getCustomer(), NotificationType.SUCCESS,
                "Hồ sơ " + c.getClaimNumber() + ": " + c.getStatus().getLabel(),
                "Số tiền được duyệt " + approved.toPlainString() + " " + c.getCurrency() + ". " + req.decisionNote(),
                NotificationService.REF_CLAIM, c.getId());
        return toDetail(actor, c);
    }

    @Transactional
    public ClaimDetail reject(UserPrincipal actor, Long id, String note) {
        requireManager(actor);
        Claim c = load(id);
        Guard.state(REVIEWABLE.contains(c.getStatus()), "Hồ sơ không ở giai đoạn thẩm định");
        c.setStatus(REJECTED);
        c.setApprovedAmount(null);
        c.setDecisionNote(note.trim());
        c.setReviewedBy(users.getReferenceById(actor.getId()));
        c.setDecidedAt(LocalDateTime.now(clock));
        notifications.notify(c.getCustomer(), NotificationType.WARNING, "Hồ sơ " + c.getClaimNumber() + " bị từ chối", note,
                NotificationService.REF_CLAIM, c.getId());
        return toDetail(actor, c);
    }

    /** Chi trả (mô phỏng) – có thể nhiều đợt, tổng không vượt số đã duyệt. */
    @Transactional
    public ClaimDetail settle(UserPrincipal actor, Long id, SettlementRequest req) {
        requireManager(actor);
        Claim c = load(id);
        Guard.state(c.getStatus() == APPROVED || c.getStatus() == PARTIALLY_APPROVED, "Chỉ chi trả hồ sơ đã được duyệt");
        BigDecimal paid = Money.of(settlements.sumPaid(c.getId()));
        BigDecimal remaining = c.getApprovedAmount().subtract(paid);
        BigDecimal amount = Money.of(req.amount());
        Guard.input(amount.compareTo(remaining) <= 0, "Số chi trả vượt quá số còn lại (" + remaining.toPlainString() + ")");
        ClaimSettlement s = new ClaimSettlement();
        s.setClaim(c);
        s.setSettlementReference(codes.next(CodeGenerator.SETTLEMENT));
        s.setAmount(amount);
        s.setCurrency(c.getCurrency());
        s.setStatus(SettlementStatus.PAID);
        s.setSettledAt(LocalDateTime.now(clock));
        s.setNote("MÔ PHỎNG" + (req.method() == null ? "" : " – " + req.method().getLabel()) + (req.note() == null ? "" : ": " + req.note()));
        s.setSettledBy(users.getReferenceById(actor.getId()));
        settlements.save(s);
        if (paid.add(amount).compareTo(c.getApprovedAmount()) >= 0) {
            c.setStatus(PAID);
        }
        notifications.notify(c.getCustomer(), NotificationType.SUCCESS,
                "Đã chi trả bồi thường " + amount.toPlainString() + " " + c.getCurrency(),
                "Hồ sơ " + c.getClaimNumber() + " – mã giao dịch mô phỏng " + s.getSettlementReference(),
                NotificationService.REF_CLAIM, c.getId());
        return toDetail(actor, c);
    }

    @Transactional
    public ClaimDetail close(UserPrincipal actor, Long id, String note) {
        requireManager(actor);
        Claim c = load(id);
        Guard.state(c.getStatus() == PAID || c.getStatus() == REJECTED, "Chỉ đóng hồ sơ đã chi trả hoặc bị từ chối");
        c.setStatus(CLOSED);
        c.setClosedAt(LocalDateTime.now(clock));
        if (note != null && !note.isBlank()) {
            c.setDecisionNote((c.getDecisionNote() == null ? "" : c.getDecisionNote() + "\n") + "Đóng hồ sơ: " + note);
        }
        return toDetail(actor, c);
    }

    // ================================================================== queries
    @Transactional(readOnly = true)
    public PageResponse<ClaimSummary> list(UserPrincipal actor, Collection<ClaimStatus> statuses, int page, int size) {
        if (!actor.isCustomer() && !actor.isManager()) {
            throw new ForbiddenException("Không có quyền xem hồ sơ bồi thường");
        }
        var result = claims.search(statuses == null || statuses.isEmpty() ? EnumSet.allOf(ClaimStatus.class) : statuses,
                actor.isCustomer() ? actor.getId() : null,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "submittedAt")));
        return PageResponse.of(result, c -> new ClaimSummary(c.getId(), c.getClaimNumber(), c.getRequest().getId(),
                c.getRequest().getRequestNumber(), c.getCustomer().getFullName(),
                c.getHorse() == null ? null : c.getHorse().getRegisteredName(),
                c.getIncident() == null ? null : c.getIncident().getIncidentNumber(), c.getClaimType(),
                c.getClaimedAmount(), c.getApprovedAmount(), c.getCurrency(), c.getStatus(), c.getSubmittedAt(),
                c.getDecidedAt()));
    }

    @Transactional(readOnly = true)
    public ClaimDetail get(UserPrincipal actor, Long id) {
        return toDetail(actor, loadVisible(actor, id));
    }

    @Transactional(readOnly = true)
    public StoredFile evidenceFile(UserPrincipal actor, Long claimId, Long evidenceId) {
        loadVisible(actor, claimId);
        ClaimEvidence e = evidences.findById(evidenceId).filter(x -> x.getClaim().getId().equals(claimId))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bằng chứng"));
        return e.getFile();
    }

    public Resource open(StoredFile f) {
        return storage.open(f);
    }

    private Claim loadVisible(UserPrincipal actor, Long id) {
        Claim c = load(id);
        boolean owner = actor.isCustomer() && c.getCustomer().getId().equals(actor.getId());
        if (!owner && !actor.isManager()) {
            throw new ForbiddenException("Bạn không có quyền xem hồ sơ bồi thường này");
        }
        return c;
    }

    public Claim load(Long id) {
        return claims.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ bồi thường"));
    }

    private static void requireManager(UserPrincipal actor) {
        if (!actor.isManager()) {
            throw new ForbiddenException("Chỉ Logistics Manager thẩm định và quyết định bồi thường");
        }
    }

    private ClaimDetail toDetail(UserPrincipal actor, Claim c) {
        BigDecimal declared = null;
        if (c.getHorse() != null) {
            declared = requestHorses.findByRequestIdFetch(c.getRequest().getId()).stream()
                    .filter(rh -> rh.getHorse().getId().equals(c.getHorse().getId()))
                    .map(RequestHorse::getDeclaredValue).findFirst().orElse(null);
        }
        List<EvidenceResponse> ev = evidences.findByClaimIdOrderByUploadedAtAsc(c.getId()).stream()
                .map(e -> new EvidenceResponse(e.getId(), e.getEvidenceType(), e.getNotes(), e.getFile().getOriginalFilename(),
                        e.getFile().getContentType(), e.getFile().getSizeBytes(), e.getUploadedBy().getFullName(), e.getUploadedAt()))
                .toList();
        List<AssessmentResponse> as = assessments.findByClaimIdOrderByAssessedAtAsc(c.getId()).stream()
                .map(a -> new AssessmentResponse(a.getId(), a.getAssessmentNote(), a.getRecommendation(),
                        a.getRecommendedAmount(), a.getAssessedBy().getFullName(), a.getAssessedAt()))
                .toList();
        List<SettlementResponse> st = settlements.findByClaimIdOrderBySettledAtAsc(c.getId()).stream()
                .map(s -> new SettlementResponse(s.getId(), s.getSettlementReference(), s.getAmount(), s.getCurrency(),
                        s.getStatus(), s.getSettledAt(), s.getNote(), s.getSettledBy().getFullName()))
                .toList();
        BigDecimal settled = st.stream().filter(s -> s.status() == SettlementStatus.PAID).map(SettlementResponse::amount)
                .reduce(Money.zero(), BigDecimal::add);
        List<String> actions = new ArrayList<>();
        boolean owner = actor.isCustomer() && c.getCustomer().getId().equals(actor.getId());
        if ((owner || actor.isManager()) && REVIEWABLE.contains(c.getStatus())) {
            actions.add("ADD_EVIDENCE");
        }
        if (actor.isManager()) {
            if (REVIEWABLE.contains(c.getStatus())) {
                actions.add("REVIEW");
                actions.add("REJECT");
            }
            if (c.getStatus() == UNDER_REVIEW) {
                actions.add("APPROVE");
            }
            if (c.getStatus() == APPROVED || c.getStatus() == PARTIALLY_APPROVED) {
                actions.add("SETTLE");
            }
            if (c.getStatus() == PAID || c.getStatus() == REJECTED) {
                actions.add("CLOSE");
            }
        }
        Incident i = c.getIncident();
        return new ClaimDetail(c.getId(), c.getClaimNumber(), c.getRequest().getId(), c.getRequest().getRequestNumber(),
                c.getTrip() == null ? null : c.getTrip().getId(), c.getTrip() == null ? null : c.getTrip().getTripNumber(),
                i == null ? null : i.getId(), i == null ? null : i.getIncidentNumber(), i == null ? null : i.getTitle(),
                i == null ? null : i.getSeverity(), c.getHorse() == null ? null : c.getHorse().getId(),
                c.getHorse() == null ? null : c.getHorse().getRegisteredName(), declared, c.getCustomer().getId(),
                c.getCustomer().getFullName(), c.getClaimType(), c.getClaimedAmount(), c.getApprovedAmount(), settled,
                c.getCurrency(), c.getDescription(), c.getStatus(), c.getSubmittedAt(),
                c.getReviewedBy() == null ? null : c.getReviewedBy().getFullName(), c.getDecidedAt(), c.getDecisionNote(),
                c.getClosedAt(), ev, as, st, actions);
    }
}
