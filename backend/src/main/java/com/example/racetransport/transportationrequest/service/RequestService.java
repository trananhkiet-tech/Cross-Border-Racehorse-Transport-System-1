package com.example.racetransport.transportationrequest.service;

import static com.example.racetransport.transportationrequest.entity.RequestStatus.*;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.common.util.Countries;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.common.util.Money;
import com.example.racetransport.compliance.service.ComplianceService;
import com.example.racetransport.document.entity.DocumentReviewStatus;
import com.example.racetransport.document.repository.DocumentRepository;
import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.horse.entity.HorseStatus;
import com.example.racetransport.horse.entity.StallType;
import com.example.racetransport.horse.repository.HorseRepository;
import com.example.racetransport.horse.repository.StallTypeRepository;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.payment.entity.InvoiceStatus;
import com.example.racetransport.payment.repository.InvoiceRepository;
import com.example.racetransport.quotation.entity.Quote;
import com.example.racetransport.quotation.entity.QuoteType;
import com.example.racetransport.quotation.repository.QuoteRepository;
import com.example.racetransport.quotation.service.EstimateService;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.transportationrequest.dto.RequestDtos.ComplianceSummary;
import com.example.racetransport.transportationrequest.dto.RequestDtos.DecisionRequest;
import com.example.racetransport.transportationrequest.dto.RequestDtos.QuoteRef;
import com.example.racetransport.transportationrequest.dto.RequestDtos.RequestDetail;
import com.example.racetransport.transportationrequest.dto.RequestDtos.RequestHorseInput;
import com.example.racetransport.transportationrequest.dto.RequestDtos.RequestHorseResponse;
import com.example.racetransport.transportationrequest.dto.RequestDtos.RequestSummary;
import com.example.racetransport.transportationrequest.dto.RequestDtos.RequestUpsert;
import com.example.racetransport.transportationrequest.dto.RequestDtos.StatusHistoryResponse;
import com.example.racetransport.transportationrequest.dto.RequestDtos.TripRef;
import com.example.racetransport.transportationrequest.entity.MovementType;
import com.example.racetransport.transportationrequest.entity.RequestHorse;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.transportationrequest.repository.RequestStatusHistoryRepository;
import com.example.racetransport.transportationrequest.repository.TransportationRequestRepository;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transportation Request: tạo/sửa/gửi bởi khách hàng, rà soát bởi Specialist,
 * phê duyệt/từ chối/yêu cầu bổ sung bởi Logistics Manager.
 */
@Service
public class RequestService {

    private final TransportationRequestRepository requests;
    private final RequestHorseRepository requestHorses;
    private final RequestStatusHistoryRepository histories;
    private final HorseRepository horses;
    private final StallTypeRepository stallTypes;
    private final AppUserRepository users;
    private final DocumentRepository documents;
    private final QuoteRepository quotes;
    private final InvoiceRepository invoices;
    private final TransportationTripRepository trips;
    private final RequestAccess access;
    private final RequestWorkflow workflow;
    private final ComplianceService complianceService;
    private final EstimateService estimateService;
    private final NotificationService notifications;
    private final CodeGenerator codes;
    private final Clock clock;

    public RequestService(TransportationRequestRepository requests, RequestHorseRepository requestHorses,
                          RequestStatusHistoryRepository histories, HorseRepository horses,
                          StallTypeRepository stallTypes, AppUserRepository users, DocumentRepository documents,
                          QuoteRepository quotes,
                          InvoiceRepository invoices, TransportationTripRepository trips, RequestAccess access,
                          RequestWorkflow workflow, ComplianceService complianceService,
                          EstimateService estimateService, NotificationService notifications,
                          CodeGenerator codes, Clock clock) {
        this.requests = requests;
        this.requestHorses = requestHorses;
        this.histories = histories;
        this.horses = horses;
        this.stallTypes = stallTypes;
        this.users = users;
        this.documents = documents;
        this.quotes = quotes;
        this.invoices = invoices;
        this.trips = trips;
        this.access = access;
        this.workflow = workflow;
        this.complianceService = complianceService;
        this.estimateService = estimateService;
        this.notifications = notifications;
        this.codes = codes;
        this.clock = clock;
    }

    // ================================================================== queries
    @Transactional(readOnly = true)
    public PageResponse<RequestSummary> list(UserPrincipal actor, Collection<RequestStatus> statuses, String q,
                                             int page, int size) {
        if (actor.isDriverEscort()) {
            throw new ForbiddenException("Tài xế / áp tải xem thông tin qua chuyến được giao");
        }
        Long customerId = actor.isCustomer() ? actor.getId() : null;
        Collection<RequestStatus> filter = statuses == null || statuses.isEmpty()
                ? EnumSet.allOf(RequestStatus.class) : statuses;
        if (!actor.isCustomer() && (statuses == null || statuses.isEmpty())) {
            // Nhân viên không thấy bản nháp của khách hàng
            filter = EnumSet.complementOf(EnumSet.of(DRAFT));
        }
        String query = q == null || q.isBlank() ? null : q.trim();
        var pageResult = requests.search(customerId, filter, query,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Long> ids = pageResult.getContent().stream().map(TransportationRequest::getId).toList();
        Map<Long, List<RequestHorse>> horsesByRequest = ids.isEmpty() ? Map.of()
                : requestHorses.findByRequestIdIn(ids).stream()
                .collect(Collectors.groupingBy(rh -> rh.getRequest().getId()));
        return PageResponse.of(pageResult, r -> toSummary(r, horsesByRequest.getOrDefault(r.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public RequestDetail get(UserPrincipal actor, Long id) {
        TransportationRequest r = access.loadVisible(actor, id);
        if (r.getStatus() == DRAFT && !actor.isCustomer()) {
            throw new ForbiddenException("Yêu cầu còn ở dạng nháp, chưa được gửi");
        }
        return toDetail(actor, r);
    }

    // ================================================================== customer actions
    @Transactional
    public RequestDetail create(UserPrincipal actor, RequestUpsert req) {
        if (!actor.isCustomer()) {
            throw new ForbiddenException("Chỉ khách hàng tạo yêu cầu vận chuyển");
        }
        validate(req);
        AppUser customer = users.getReferenceById(actor.getId());
        TransportationRequest r = new TransportationRequest();
        r.setCustomer(customer);
        r.setRequestNumber(codes.next(CodeGenerator.REQUEST));
        r.setStatus(DRAFT);
        apply(r, req);
        requests.save(r);
        replaceHorses(actor, r, req.horses());
        workflow.recordCreation(r, customer);
        return toDetail(actor, r);
    }

    @Transactional
    public RequestDetail update(UserPrincipal actor, Long id, RequestUpsert req) {
        TransportationRequest r = access.loadOwned(actor, id);
        Guard.state(RequestWorkflow.EDITABLE.contains(r.getStatus()),
                "Chỉ sửa được yêu cầu ở trạng thái Bản nháp hoặc Cần bổ sung thông tin");
        validate(req);
        apply(r, req);
        replaceHorses(actor, r, req.horses());
        return toDetail(actor, r);
    }

    /** Gửi yêu cầu: sinh checklist tuân thủ + chi phí ước tính, thông báo Specialist. */
    @Transactional
    public RequestDetail submit(UserPrincipal actor, Long id) {
        TransportationRequest r = access.loadOwned(actor, id);
        AppUser customer = users.getReferenceById(actor.getId());
        Guard.state(requestHorses.countByRequestId(r.getId()) > 0, "Yêu cầu chưa có ngựa nào");
        Guard.state(!r.getRequestedDepartureDate().isBefore(LocalDate.now(clock)),
                "Ngày khởi hành dự kiến đã qua, vui lòng cập nhật");
        boolean resubmit = r.getStatus() == INFORMATION_REQUIRED;
        workflow.transition(r, SUBMITTED, customer, resubmit ? "Khách hàng gửi lại sau khi bổ sung" : "Khách hàng gửi yêu cầu");
        complianceService.generateChecklist(r, customer);
        estimateService.generateEstimate(r, customer);
        notifications.notifyRole(Role.TRANSPORT_SPECIALIST, NotificationType.ACTION_REQUIRED,
                (resubmit ? "Yêu cầu được gửi lại: " : "Yêu cầu mới cần rà soát: ") + r.getRequestNumber(),
                "Khách hàng " + r.getCustomer().getFullName() + " đã gửi yêu cầu vận chuyển "
                        + Countries.name(r.getOriginCountry()) + " → " + Countries.name(r.getDestinationCountry()) + ".",
                NotificationService.REF_REQUEST, r.getId());
        return toDetail(actor, r);
    }

    @Transactional
    public RequestDetail cancel(UserPrincipal actor, Long id, DecisionRequest body) {
        TransportationRequest r = actor.isCustomer() ? access.loadOwned(actor, id) : access.load(id);
        if (!actor.isCustomer() && !actor.isManager()) {
            throw new ForbiddenException("Chỉ khách hàng hoặc Logistics Manager được hủy yêu cầu");
        }
        Guard.state(EnumSet.of(DRAFT, SUBMITTED, UNDER_DOCUMENT_REVIEW, INFORMATION_REQUIRED, COMPLIANCE_REVIEW,
                        PENDING_MANAGER_APPROVAL, APPROVED, QUOTE_SENT, QUOTE_REJECTED).contains(r.getStatus()),
                "Không thể hủy yêu cầu sau khi đã chấp nhận báo giá");
        AppUser user = users.getReferenceById(actor.getId());
        String note = body == null || body.note() == null || body.note().isBlank() ? "Hủy yêu cầu" : body.note();
        workflow.transition(r, CANCELLED, user, note);
        r.setLastDecisionNote(note);
        estimateService.closeOpenQuotes(r);
        if (actor.isManager()) {
            notifications.notify(r.getCustomer(), NotificationType.WARNING,
                    "Yêu cầu " + r.getRequestNumber() + " đã bị hủy", note, NotificationService.REF_REQUEST, r.getId());
        } else if (r.getSubmittedAt() != null) {
            notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.INFO,
                    "Khách hàng hủy yêu cầu " + r.getRequestNumber(), note, NotificationService.REF_REQUEST, r.getId());
        }
        return toDetail(actor, r);
    }

    // ================================================================== specialist actions
    @Transactional
    public RequestDetail startDocumentReview(UserPrincipal actor, Long id) {
        requireRole(actor, Role.TRANSPORT_SPECIALIST);
        TransportationRequest r = access.load(id);
        workflow.transition(r, UNDER_DOCUMENT_REVIEW, users.getReferenceById(actor.getId()), "Bắt đầu rà soát tài liệu");
        return toDetail(actor, r);
    }

    @Transactional
    public RequestDetail completeDocumentReview(UserPrincipal actor, Long id, DecisionRequest body) {
        requireRole(actor, Role.TRANSPORT_SPECIALIST);
        TransportationRequest r = access.load(id);
        long pending = documents.countByRequestIdAndReviewStatus(r.getId(), DocumentReviewStatus.PENDING_REVIEW);
        Guard.state(pending == 0, "Còn " + pending + " tài liệu chờ rà soát");
        workflow.transition(r, COMPLIANCE_REVIEW, users.getReferenceById(actor.getId()),
                noteOr(body, "Hoàn tất rà soát tài liệu"));
        return toDetail(actor, r);
    }

    /** Xác nhận đủ hồ sơ tuân thủ → chuyển Manager phê duyệt. */
    @Transactional
    public RequestDetail completeCompliance(UserPrincipal actor, Long id, DecisionRequest body) {
        requireRole(actor, Role.TRANSPORT_SPECIALIST);
        TransportationRequest r = access.load(id);
        ComplianceSummary summary = complianceSummary(r.getId());
        Guard.state(summary.total() > 0, "Yêu cầu chưa có checklist tuân thủ");
        Guard.state(summary.complete(), "Checklist chưa đạt: còn " + (summary.total() - summary.approved())
                + " mục bắt buộc chưa được duyệt hoặc miễn trừ");
        workflow.transition(r, PENDING_MANAGER_APPROVAL, users.getReferenceById(actor.getId()),
                noteOr(body, "Hồ sơ tuân thủ đầy đủ"));
        notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.ACTION_REQUIRED,
                "Chờ phê duyệt: " + r.getRequestNumber(),
                "Hồ sơ tuân thủ của yêu cầu " + r.getRequestNumber() + " đã đầy đủ, cần quyết định phê duyệt.",
                NotificationService.REF_REQUEST, r.getId());
        return toDetail(actor, r);
    }

    // ================================================================== manager / specialist decisions
    @Transactional
    public RequestDetail requestInformation(UserPrincipal actor, Long id, DecisionRequest body) {
        if (!actor.isManager() && !actor.isSpecialist()) {
            throw new ForbiddenException("Chỉ Specialist hoặc Logistics Manager được yêu cầu bổ sung");
        }
        String note = requireNote(body, "Cần ghi rõ thông tin/tài liệu cần bổ sung");
        TransportationRequest r = access.load(id);
        workflow.transition(r, INFORMATION_REQUIRED, users.getReferenceById(actor.getId()), note);
        r.setLastDecisionNote(note);
        notifications.notify(r.getCustomer(), NotificationType.ACTION_REQUIRED,
                "Cần bổ sung thông tin cho " + r.getRequestNumber(), note, NotificationService.REF_REQUEST, r.getId());
        return toDetail(actor, r);
    }

    @Transactional
    public RequestDetail approve(UserPrincipal actor, Long id, DecisionRequest body) {
        requireRole(actor, Role.LOGISTICS_MANAGER);
        TransportationRequest r = access.load(id);
        Guard.state(r.getStatus() == PENDING_MANAGER_APPROVAL,
                "Chỉ phê duyệt được yêu cầu ở trạng thái Chờ quản lý phê duyệt");
        String note = noteOr(body, "Phê duyệt yêu cầu");
        workflow.transition(r, APPROVED, users.getReferenceById(actor.getId()), note);
        r.setLastDecisionNote(note);
        notifications.notify(r.getCustomer(), NotificationType.SUCCESS,
                "Yêu cầu " + r.getRequestNumber() + " đã được phê duyệt",
                "Báo giá cuối sẽ được gửi sau khi đội vận hành xác nhận chi phí.",
                NotificationService.REF_REQUEST, r.getId());
        return toDetail(actor, r);
    }

    @Transactional
    public RequestDetail reject(UserPrincipal actor, Long id, DecisionRequest body) {
        requireRole(actor, Role.LOGISTICS_MANAGER);
        String note = requireNote(body, "Cần ghi lý do từ chối");
        TransportationRequest r = access.load(id);
        workflow.transition(r, REJECTED, users.getReferenceById(actor.getId()), note);
        r.setLastDecisionNote(note);
        estimateService.closeOpenQuotes(r);
        notifications.notify(r.getCustomer(), NotificationType.WARNING,
                "Yêu cầu " + r.getRequestNumber() + " bị từ chối", note, NotificationService.REF_REQUEST, r.getId());
        return toDetail(actor, r);
    }

    // ================================================================== helpers
    private void validate(RequestUpsert req) {
        Guard.input(Countries.isSupported(req.originCountry()), "Quốc gia đi không được hỗ trợ");
        Guard.input(Countries.isSupported(req.destinationCountry()), "Quốc gia đến không được hỗ trợ");
        Guard.input(!req.requestedDepartureDate().isBefore(LocalDate.now(clock)),
                "Ngày khởi hành dự kiến phải từ hôm nay trở đi");
        if (req.movementType() == MovementType.TEMPORARY) {
            Guard.input(req.returnDate() != null, "Di chuyển tạm thời cần ngày quay về dự kiến");
        }
        if (req.returnDate() != null) {
            Guard.input(req.returnDate().isAfter(req.requestedDepartureDate()), "Ngày quay về phải sau ngày khởi hành");
        }
        Set<Long> seen = new HashSet<>();
        for (RequestHorseInput h : req.horses()) {
            Guard.input(seen.add(h.horseId()), "Một ngựa bị chọn hai lần");
        }
    }

    private void apply(TransportationRequest r, RequestUpsert req) {
        r.setOriginCountry(req.originCountry());
        r.setOriginAddress(req.originAddress().trim());
        r.setDestinationCountry(req.destinationCountry());
        r.setDestinationAddress(req.destinationAddress().trim());
        r.setRequestedDepartureDate(req.requestedDepartureDate());
        r.setReturnDate(req.movementType() == MovementType.TEMPORARY ? req.returnDate() : null);
        r.setMovementType(req.movementType());
        r.setPurpose(req.purpose());
        r.setTransportMode(req.transportMode());
        r.setCurrency(req.currency() == null ? "USD" : req.currency());
        r.setSpecialRequirements(req.specialRequirements());
        r.setNotes(req.notes());
    }

    /** Thay toàn bộ danh sách ngựa; tổng giá trị khai báo do backend tự cộng. */
    private void replaceHorses(UserPrincipal actor, TransportationRequest r, List<RequestHorseInput> inputs) {
        requestHorses.deleteByRequestId(r.getId());
        requestHorses.flush();
        BigDecimal total = Money.zero();
        for (RequestHorseInput in : inputs) {
            Horse horse = horses.findById(in.horseId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy ngựa #" + in.horseId()));
            if (!horse.getOwner().getId().equals(actor.getId())) {
                throw new ForbiddenException("Ngựa " + horse.getRegisteredName() + " không thuộc tài khoản của bạn");
            }
            Guard.input(horse.getStatus() == HorseStatus.ACTIVE,
                    "Ngựa " + horse.getRegisteredName() + " không ở trạng thái sẵn sàng");
            StallType stall = stallTypes.findById(in.stallTypeId())
                    .filter(s -> Boolean.TRUE.equals(s.getActive()))
                    .orElseThrow(() -> new NotFoundException("Loại chuồng không hợp lệ"));
            RequestHorse rh = new RequestHorse();
            rh.setRequest(r);
            rh.setHorse(horse);
            rh.setStallType(stall);
            rh.setDeclaredValue(Money.of(in.declaredValue()));
            rh.setNotes(in.notes());
            requestHorses.save(rh);
            total = total.add(rh.getDeclaredValue());
        }
        r.setDeclaredValueTotal(total);
    }

    private static void requireRole(UserPrincipal actor, Role role) {
        if (!actor.hasRole(role)) {
            throw new ForbiddenException("Thao tác dành cho " + role.getLabel());
        }
    }

    private static String requireNote(DecisionRequest body, String message) {
        Guard.input(body != null && body.note() != null && !body.note().isBlank(), message);
        return body.note().trim();
    }

    private static String noteOr(DecisionRequest body, String fallback) {
        return body == null || body.note() == null || body.note().isBlank() ? fallback : body.note().trim();
    }

    public ComplianceSummary complianceSummary(Long requestId) {
        return complianceService.summary(requestId);
    }

    private RequestSummary toSummary(TransportationRequest r, List<RequestHorse> rhs) {
        return new RequestSummary(r.getId(), r.getRequestNumber(), r.getCustomer().getId(),
                r.getCustomer().getFullName(), r.getOriginCountry(), r.getOriginAddress(), r.getDestinationCountry(),
                r.getDestinationAddress(), r.getRequestedDepartureDate(), r.getMovementType(), r.getPurpose(),
                r.getTransportMode(), r.getStatus(), rhs.size(),
                rhs.stream().map(rh -> rh.getHorse().getRegisteredName()).toList(),
                r.getDeclaredValueTotal(), r.getCurrency(), r.getCreatedAt(), r.getSubmittedAt());
    }

    private RequestDetail toDetail(UserPrincipal actor, TransportationRequest r) {
        List<RequestHorseResponse> horseList = requestHorses.findByRequestIdFetch(r.getId()).stream()
                .map(rh -> new RequestHorseResponse(rh.getId(), rh.getHorse().getId(),
                        rh.getHorse().getRegisteredName(), rh.getHorse().getMicrochipNumber(),
                        rh.getHorse().getPassportNumber(), rh.getHorse().getBreed(), rh.getStallType().getId(),
                        rh.getStallType().getCode(), rh.getStallType().getName(), rh.getStallType().getWidthCm(),
                        rh.getStallType().getSurcharge(), rh.getDeclaredValue(), rh.getNotes()))
                .toList();
        List<StatusHistoryResponse> history = histories.findByRequestIdOrderByChangedAtAscIdAsc(r.getId()).stream()
                .map(h -> new StatusHistoryResponse(h.getFromStatus(), h.getToStatus(), h.getChangedBy().getFullName(),
                        h.getChangedBy().getRole(), h.getNote(), h.getChangedAt()))
                .toList();
        QuoteRef estimate = quotes.findFirstByRequestIdAndQuoteTypeOrderByVersionNoDesc(r.getId(), QuoteType.ESTIMATE)
                .map(RequestService::toQuoteRef).orElse(null);
        QuoteRef latest = quotes.findFirstByRequestIdAndQuoteTypeOrderByVersionNoDesc(r.getId(), QuoteType.FINAL)
                .filter(q -> !actor.isCustomer() || q.getSentAt() != null)
                .map(RequestService::toQuoteRef).orElse(null);
        Long invoiceId = invoices.findFirstByRequestIdAndStatusNotOrderByIdDesc(r.getId(), InvoiceStatus.CANCELLED)
                .map(i -> i.getId()).orElse(null);
        List<TripRef> tripRefs = trips.findByRequestIdOrderByPlannedDepartureAsc(r.getId()).stream()
                .map(t -> new TripRef(t.getId(), t.getTripNumber(), t.getStatus(), t.getPlannedDeparture()))
                .toList();
        ComplianceSummary compliance = complianceSummary(r.getId());
        AppUser c = r.getCustomer();
        return new RequestDetail(r.getId(), r.getRequestNumber(), c.getId(), c.getFullName(), c.getEmail(),
                c.getPhone(), r.getOriginCountry(), r.getOriginAddress(), r.getDestinationCountry(),
                r.getDestinationAddress(), r.getRequestedDepartureDate(), r.getReturnDate(), r.getMovementType(),
                r.getPurpose(), r.getTransportMode(), r.getDeclaredValueTotal(), r.getCurrency(),
                r.getSpecialRequirements(), r.getNotes(), r.getStatus(), r.getLastDecisionNote(), r.getCreatedAt(),
                r.getSubmittedAt(), r.getApprovedAt(), r.getCompletedAt(), horseList, history, compliance, estimate,
                latest, invoiceId, tripRefs, allowedActions(actor, r, compliance, latest));
    }

    private static QuoteRef toQuoteRef(Quote q) {
        return new QuoteRef(q.getId(), q.getQuoteNumber(), q.getVersionNo(), q.getStatus(), q.getTotalAmount(),
                q.getCurrency());
    }

    /** Danh sách hành động frontend được phép hiển thị (backend vẫn kiểm tra lại khi gọi). */
    private List<String> allowedActions(UserPrincipal actor, TransportationRequest r, ComplianceSummary compliance,
                                        QuoteRef latest) {
        List<String> actions = new ArrayList<>();
        RequestStatus s = r.getStatus();
        if (access.isOwner(actor, r)) {
            if (RequestWorkflow.EDITABLE.contains(s)) {
                actions.add("EDIT");
                actions.add("SUBMIT");
                actions.add("UPLOAD_DOCUMENT");
            }
            if (RequestWorkflow.IN_REVIEW.contains(s)) {
                actions.add("UPLOAD_DOCUMENT");
            }
            if (EnumSet.of(DRAFT, SUBMITTED, UNDER_DOCUMENT_REVIEW, INFORMATION_REQUIRED, COMPLIANCE_REVIEW,
                    PENDING_MANAGER_APPROVAL, APPROVED, QUOTE_SENT, QUOTE_REJECTED).contains(s)) {
                actions.add("CANCEL");
            }
            if (s == QUOTE_SENT && latest != null) {
                actions.add("RESPOND_QUOTE");
            }
            if (EnumSet.of(TRIP_PLANNED, COMPLETED).contains(s)) {
                actions.add("CREATE_CLAIM");
            }
        }
        if (actor.isSpecialist()) {
            if (s == SUBMITTED) {
                actions.add("START_REVIEW");
            }
            if (s == UNDER_DOCUMENT_REVIEW) {
                actions.add("COMPLETE_DOCUMENT_REVIEW");
            }
            if (s == COMPLIANCE_REVIEW && compliance.complete()) {
                actions.add("COMPLETE_COMPLIANCE");
            }
            if (EnumSet.of(SUBMITTED, UNDER_DOCUMENT_REVIEW, COMPLIANCE_REVIEW).contains(s)) {
                actions.add("REVIEW_DOCUMENTS");
                actions.add("REQUEST_INFORMATION");
            }
        }
        if (actor.isManager()) {
            if (s == PENDING_MANAGER_APPROVAL) {
                actions.add("APPROVE");
            }
            if (RequestWorkflow.IN_REVIEW.contains(s) || s == INFORMATION_REQUIRED) {
                actions.add("REJECT");
            }
            if (RequestWorkflow.IN_REVIEW.contains(s)) {
                actions.add("REQUEST_INFORMATION");
            }
            if (EnumSet.of(APPROVED, QUOTE_SENT, QUOTE_REJECTED).contains(s)) {
                actions.add("BUILD_QUOTE");
            }
            if (!RequestWorkflow.TERMINAL.contains(s) && s != QUOTE_ACCEPTED && s != READY_FOR_PLANNING
                    && s != TRIP_PLANNED) {
                actions.add("CANCEL");
            }
        }
        if (actor.isCoordinator() && EnumSet.of(READY_FOR_PLANNING, TRIP_PLANNED).contains(s)) {
            actions.add("PLAN_TRIP");
        }
        return actions;
    }
}
