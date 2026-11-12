package com.example.racetransport.compliance.service;

import static com.example.racetransport.compliance.entity.ChecklistItemStatus.*;

import com.example.racetransport.common.exception.ConflictException;
import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.util.Countries;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.compliance.dto.ComplianceDtos;
import com.example.racetransport.compliance.dto.ComplianceDtos.ChecklistItemResponse;
import com.example.racetransport.compliance.dto.ComplianceDtos.ChecklistResponse;
import com.example.racetransport.compliance.dto.ComplianceDtos.OpenItemResponse;
import com.example.racetransport.compliance.dto.ComplianceDtos.PatchChecklistItemRequest;
import com.example.racetransport.compliance.dto.ComplianceDtos.RequirementRequest;
import com.example.racetransport.compliance.dto.ComplianceDtos.RequirementResponse;
import com.example.racetransport.compliance.entity.ChecklistItemStatus;
import com.example.racetransport.compliance.entity.ComplianceCategory;
import com.example.racetransport.compliance.entity.ComplianceChecklistItem;
import com.example.racetransport.compliance.entity.ComplianceRequirement;
import com.example.racetransport.compliance.repository.ComplianceChecklistItemRepository;
import com.example.racetransport.compliance.repository.ComplianceRequirementRepository;
import com.example.racetransport.document.entity.Document;
import com.example.racetransport.document.entity.DocumentReviewStatus;
import com.example.racetransport.document.repository.DocumentRepository;
import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.transportationrequest.dto.RequestDtos.ComplianceSummary;
import com.example.racetransport.transportationrequest.entity.RequestHorse;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.transportationrequest.service.RequestAccess;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compliance Requirement (danh mục DEMO theo tuyến) và Compliance Checklist của từng yêu cầu.
 * Không hard-code mọi quốc gia giống nhau: yêu cầu lọc theo nước đi/đến, loại di chuyển, mục đích và hiệu lực.
 */
@Service
public class ComplianceService {

    /** Trạng thái mục "chưa ổn" cần khách hàng bổ sung. */
    public static final Set<ChecklistItemStatus> OPEN_STATUSES = EnumSet.of(MISSING, REJECTED, NEEDS_UPDATE, EXPIRED);

    /** Yêu cầu còn đang trong quá trình xét hồ sơ (được cập nhật checklist). */
    private static final Set<RequestStatus> REVIEWABLE = EnumSet.of(RequestStatus.SUBMITTED,
            RequestStatus.UNDER_DOCUMENT_REVIEW, RequestStatus.INFORMATION_REQUIRED, RequestStatus.COMPLIANCE_REVIEW,
            RequestStatus.PENDING_MANAGER_APPROVAL);

    private static final int DUE_DAYS_BEFORE_DEPARTURE = 7;

    private final ComplianceRequirementRepository requirements;
    private final ComplianceChecklistItemRepository items;
    private final RequestHorseRepository requestHorses;
    private final DocumentRepository documents;
    private final RequestAccess requestAccess;
    private final NotificationService notifications;
    private final AppUserRepository users;
    private final Clock clock;

    public ComplianceService(ComplianceRequirementRepository requirements, ComplianceChecklistItemRepository items,
                             RequestHorseRepository requestHorses, DocumentRepository documents,
                             RequestAccess requestAccess, NotificationService notifications, AppUserRepository users,
                             Clock clock) {
        this.requirements = requirements;
        this.items = items;
        this.requestHorses = requestHorses;
        this.documents = documents;
        this.requestAccess = requestAccess;
        this.notifications = notifications;
        this.users = users;
        this.clock = clock;
    }

    // ================================================================== checklist generation
    /**
     * Sinh (hoặc đồng bộ lại) checklist cho yêu cầu. Idempotent:
     * - thêm mục còn thiếu theo yêu cầu áp dụng (per-horse → mỗi ngựa một mục);
     * - xóa mục MISSING không còn áp dụng (ví dụ khách bỏ bớt ngựa);
     * - tự gắn tài liệu đã tải của yêu cầu, hoặc tài liệu cấp ngựa đã duyệt và còn hạn.
     * @return số mục mới được tạo
     */
    @Transactional
    public int generateChecklist(TransportationRequest request, AppUser actor) {
        LocalDate departure = request.getRequestedDepartureDate();
        List<ComplianceRequirement> applicable = requirements.findApplicable(request.getOriginCountry(),
                request.getDestinationCountry(), request.getMovementType(), request.getPurpose(), departure);
        List<Horse> horses = requestHorses.findByRequestIdFetch(request.getId()).stream()
                .map(RequestHorse::getHorse).toList();
        List<ComplianceChecklistItem> existing = items.findByRequestId(request.getId());

        // Khóa (requirementId, horseId) cần có
        Set<String> wanted = new java.util.HashSet<>();
        for (ComplianceRequirement req : applicable) {
            if (Boolean.TRUE.equals(req.getPerHorse())) {
                horses.forEach(h -> wanted.add(key(req.getId(), h.getId())));
            } else {
                wanted.add(key(req.getId(), null));
            }
        }
        // Xóa mục không còn áp dụng và chưa có tài liệu
        for (ComplianceChecklistItem it : existing) {
            String k = key(it.getRequirement().getId(), it.getHorse() == null ? null : it.getHorse().getId());
            if (!wanted.contains(k) && it.getStatus() == MISSING && it.getDocument() == null) {
                items.delete(it);
            }
        }
        Set<String> have = existing.stream()
                .map(it -> key(it.getRequirement().getId(), it.getHorse() == null ? null : it.getHorse().getId()))
                .collect(Collectors.toSet());
        int created = 0;
        List<Document> requestDocs = documents.findByRequestIdOrderByCreatedAtDesc(request.getId());
        for (ComplianceRequirement req : applicable) {
            List<Horse> targets = Boolean.TRUE.equals(req.getPerHorse()) ? horses : java.util.Collections.singletonList(null);
            for (Horse h : targets) {
                String k = key(req.getId(), h == null ? null : h.getId());
                if (have.contains(k)) {
                    continue;
                }
                ComplianceChecklistItem it = new ComplianceChecklistItem();
                it.setRequest(request);
                it.setRequirement(req);
                it.setHorse(h);
                it.setStatus(MISSING);
                it.setDueDate(departure.minusDays(DUE_DAYS_BEFORE_DEPARTURE));
                autoLink(it, requestDocs, departure);
                items.save(it);
                created++;
            }
        }
        return created;
    }

    /** Gắn tài liệu sẵn có cho mục mới: ưu tiên tài liệu của yêu cầu, sau đó tài liệu cấp ngựa đã duyệt. */
    private void autoLink(ComplianceChecklistItem it, List<Document> requestDocs, LocalDate departure) {
        var type = it.getRequirement().getDocumentType();
        Long horseId = it.getHorse() == null ? null : it.getHorse().getId();
        for (Document d : requestDocs) {
            if (d.getDocumentType() == type && Objects.equals(horseId, d.getHorse() == null ? null : d.getHorse().getId())
                    && d.getReviewStatus() != DocumentReviewStatus.REJECTED) {
                linkDocument(it, d);
                return;
            }
        }
        if (horseId != null) {
            for (Document d : documents.findByHorseIdOrderByCreatedAtDesc(horseId)) {
                if (d.getDocumentType() == type && d.getReviewStatus() == DocumentReviewStatus.APPROVED
                        && (d.getExpiryDate() == null || !d.getExpiryDate().isBefore(departure))
                        && withinValidity(it.getRequirement(), d, departure)) {
                    it.setDocument(d);
                    it.setStatus(APPROVED);
                    it.setReviewerNote("Dùng lại tài liệu đã duyệt của ngựa");
                    it.setCheckedAt(LocalDateTime.now(clock));
                    return;
                }
            }
        }
    }

    private static boolean withinValidity(ComplianceRequirement req, Document d, LocalDate departure) {
        if (req.getValidityDays() == null || d.getIssueDate() == null) {
            return true;
        }
        return !d.getIssueDate().isBefore(departure.minusDays(req.getValidityDays()));
    }

    private void linkDocument(ComplianceChecklistItem it, Document d) {
        it.setDocument(d);
        switch (d.getReviewStatus()) {
            case APPROVED -> it.setStatus(APPROVED);
            case PENDING_REVIEW -> it.setStatus(SUBMITTED);
            case NEEDS_UPDATE -> it.setStatus(NEEDS_UPDATE);
            case EXPIRED -> it.setStatus(EXPIRED);
            case REJECTED -> it.setStatus(REJECTED);
            default -> it.setStatus(SUBMITTED);
        }
    }

    /** Gọi khi tài liệu mới được tải lên cho một yêu cầu. */
    @Transactional
    public void attachDocument(TransportationRequest request, Document d, Long checklistItemId) {
        if (checklistItemId != null) {
            ComplianceChecklistItem it = items.findById(checklistItemId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy mục checklist"));
            Guard.input(it.getRequest().getId().equals(request.getId()), "Mục checklist không thuộc yêu cầu này");
            Guard.input(it.getRequirement().getDocumentType() == d.getDocumentType(),
                    "Loại tài liệu không khớp với mục checklist (" + it.getRequirement().getDocumentType().getLabel() + ")");
            if (it.getHorse() != null) {
                Guard.input(d.getHorse() != null && d.getHorse().getId().equals(it.getHorse().getId()),
                        "Tài liệu phải gắn với ngựa " + it.getHorse().getRegisteredName());
            }
            Guard.state(it.getStatus() != APPROVED && it.getStatus() != WAIVED, "Mục checklist này đã đạt");
            linkDocument(it, d);
            return;
        }
        Long horseId = d.getHorse() == null ? null : d.getHorse().getId();
        for (ComplianceChecklistItem it : items.findByRequestId(request.getId())) {
            Long itemHorse = it.getHorse() == null ? null : it.getHorse().getId();
            if (it.getRequirement().getDocumentType() == d.getDocumentType() && Objects.equals(itemHorse, horseId)
                    && OPEN_STATUSES.contains(it.getStatus())) {
                linkDocument(it, d);
            }
        }
    }

    /** Đồng bộ trạng thái mục checklist theo kết quả rà soát tài liệu. */
    @Transactional
    public void onDocumentReviewed(Document d, AppUser reviewer) {
        for (ComplianceChecklistItem it : items.findByDocumentId(d.getId())) {
            linkDocument(it, d);
            if (d.getReviewStatus() == DocumentReviewStatus.APPROVED && d.getRequest() != null
                    && !withinValidity(it.getRequirement(), d, d.getRequest().getRequestedDepartureDate())) {
                it.setStatus(NEEDS_UPDATE);
                it.setReviewerNote("Tài liệu cấp quá " + it.getRequirement().getValidityDays()
                        + " ngày trước ngày khởi hành");
            } else {
                it.setReviewerNote(d.getReviewNote());
            }
            it.setCheckedBy(reviewer);
            it.setCheckedAt(LocalDateTime.now(clock));
        }
    }

    // ================================================================== checklist API
    @Transactional(readOnly = true)
    public ChecklistResponse getChecklist(UserPrincipal actor, Long requestId) {
        TransportationRequest r = requestAccess.loadVisible(actor, requestId);
        List<ChecklistItemResponse> list = items.findByRequestIdFetch(requestId).stream().map(this::toItem).toList();
        return new ChecklistResponse(r.getId(), r.getRequestNumber(), r.getStatus(),
                Countries.name(r.getOriginCountry()) + " → " + Countries.name(r.getDestinationCountry()),
                r.getRequestedDepartureDate(), summary(requestId), list, ComplianceDtos.DISCLAIMER);
    }

    /** Specialist/Manager sinh lại checklist (ví dụ sau khi cập nhật danh mục yêu cầu). */
    @Transactional
    public ChecklistResponse regenerate(UserPrincipal actor, Long requestId) {
        if (!actor.isSpecialist() && !actor.isManager()) {
            throw new ForbiddenException("Chỉ Specialist hoặc Manager được sinh checklist");
        }
        TransportationRequest r = requestAccess.load(requestId);
        Guard.state(r.getStatus() != RequestStatus.DRAFT, "Yêu cầu chưa được gửi");
        Guard.state(REVIEWABLE.contains(r.getStatus()), "Yêu cầu đã qua giai đoạn xét hồ sơ");
        generateChecklist(r, users.getReferenceById(actor.getId()));
        return getChecklist(actor, requestId);
    }

    @Transactional
    public ChecklistItemResponse patchItem(UserPrincipal actor, Long itemId, PatchChecklistItemRequest req) {
        if (!actor.isSpecialist()) {
            throw new ForbiddenException("Chỉ Transport Specialist cập nhật checklist");
        }
        ComplianceChecklistItem it = items.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mục checklist"));
        Guard.state(REVIEWABLE.contains(it.getRequest().getStatus()), "Yêu cầu không còn ở giai đoạn xét hồ sơ");
        ChecklistItemStatus to = req.status();
        Guard.input(to != SUBMITTED, "Trạng thái 'Đã nộp' do hệ thống đặt khi có tài liệu");
        if (to != APPROVED && to != MISSING) {
            Guard.input(req.note() != null && !req.note().isBlank(), "Cần ghi chú cho kết quả này");
        }
        if (req.documentId() != null) {
            Document d = documents.findById(req.documentId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy tài liệu"));
            Guard.input(d.getRequest() != null && d.getRequest().getId().equals(it.getRequest().getId())
                    || (d.getHorse() != null && it.getHorse() != null && d.getHorse().getId().equals(it.getHorse().getId())),
                    "Tài liệu không thuộc yêu cầu/ngựa của mục này");
            Guard.input(d.getDocumentType() == it.getRequirement().getDocumentType(), "Loại tài liệu không khớp");
            it.setDocument(d);
        }
        if (to == APPROVED) {
            Guard.state(it.getDocument() != null && it.getDocument().getReviewStatus() == DocumentReviewStatus.APPROVED,
                    "Chỉ đánh dấu Đạt khi tài liệu minh chứng đã được duyệt");
        }
        if (to == WAIVED) {
            Guard.state(!Boolean.TRUE.equals(it.getRequirement().getMandatory())
                            || (req.note() != null && req.note().length() >= 10),
                    "Miễn trừ mục bắt buộc cần lý do rõ ràng (≥ 10 ký tự)");
        }
        if (to == MISSING) {
            it.setDocument(null);
        }
        it.setStatus(to);
        it.setReviewerNote(req.note());
        it.setCheckedBy(users.getReferenceById(actor.getId()));
        it.setCheckedAt(LocalDateTime.now(clock));
        return toItem(it);
    }

    /** Gửi thông báo cho khách danh sách tài liệu còn thiếu/cần cập nhật. */
    @Transactional
    public int notifyMissing(UserPrincipal actor, Long requestId) {
        if (!actor.isSpecialist()) {
            throw new ForbiddenException("Chỉ Transport Specialist");
        }
        TransportationRequest r = requestAccess.load(requestId);
        List<ComplianceChecklistItem> open = items.findByRequestIdFetch(requestId).stream()
                .filter(i -> OPEN_STATUSES.contains(i.getStatus())).toList();
        Guard.state(!open.isEmpty(), "Không có mục nào cần bổ sung");
        String list = open.stream().map(i -> "• " + i.getRequirement().getTitle()
                        + (i.getHorse() == null ? "" : " (" + i.getHorse().getRegisteredName() + ")")
                        + " – " + i.getStatus().getLabel())
                .collect(Collectors.joining("\n"));
        notifications.notify(r.getCustomer(), NotificationType.ACTION_REQUIRED,
                "Hồ sơ " + r.getRequestNumber() + " cần bổ sung " + open.size() + " tài liệu", list,
                NotificationService.REF_REQUEST, r.getId());
        return open.size();
    }

    @Transactional(readOnly = true)
    public List<OpenItemResponse> openItems(UserPrincipal actor) {
        if (actor.isDriverEscort() || actor.isCoordinator()) {
            throw new ForbiddenException("Không có quyền xem danh sách hồ sơ thiếu");
        }
        Long customerId = actor.isCustomer() ? actor.getId() : null;
        return items.findOpenItems(OPEN_STATUSES, customerId).stream()
                .map(i -> new OpenItemResponse(i.getId(), i.getRequest().getId(), i.getRequest().getRequestNumber(),
                        i.getRequest().getCustomer().getFullName(), i.getRequest().getRequestedDepartureDate(),
                        i.getRequirement().getTitle(), i.getRequirement().getDocumentType(),
                        i.getRequirement().getCategory(),
                        i.getHorse() == null ? null : i.getHorse().getRegisteredName(), i.getStatus(),
                        i.getReviewerNote(), i.getDueDate()))
                .toList();
    }

    /** Tiến độ tuân thủ: chỉ mục bắt buộc quyết định điều kiện "đủ hồ sơ". */
    @Transactional(readOnly = true)
    public ComplianceSummary summary(Long requestId) {
        long total = 0;
        long approved = 0;
        long pending = 0;
        long missing = 0;
        long problems = 0;
        for (Object[] row : items.countByStatus(requestId)) {
            ChecklistItemStatus st = (ChecklistItemStatus) row[0];
            boolean mandatory = Boolean.TRUE.equals(row[1]);
            long n = ((Number) row[2]).longValue();
            if (!mandatory) {
                continue;
            }
            total += n;
            switch (st) {
                case APPROVED, WAIVED -> approved += n;
                case SUBMITTED -> pending += n;
                case MISSING -> missing += n;
                default -> problems += n;
            }
        }
        return new ComplianceSummary(total, approved, pending, missing, problems, total > 0 && approved == total);
    }

    // ================================================================== requirements catalog
    @Transactional(readOnly = true)
    public List<RequirementResponse> listRequirements(Collection<ComplianceCategory> categories, String destination,
                                                      boolean includeInactive) {
        return requirements.search(categories == null || categories.isEmpty()
                                ? EnumSet.allOf(ComplianceCategory.class) : categories,
                        destination == null || destination.isBlank() ? null : destination, includeInactive)
                .stream().map(ComplianceService::toRequirement).toList();
    }

    @Transactional
    public RequirementResponse createRequirement(UserPrincipal actor, RequirementRequest req) {
        requireSpecialist(actor);
        if (requirements.existsByCode(req.code())) {
            throw new ConflictException("Mã yêu cầu tuân thủ đã tồn tại");
        }
        ComplianceRequirement c = new ComplianceRequirement();
        applyRequirement(c, req);
        requirements.save(c);
        return toRequirement(c);
    }

    @Transactional
    public RequirementResponse updateRequirement(UserPrincipal actor, Long id, RequirementRequest req) {
        requireSpecialist(actor);
        ComplianceRequirement c = requirements.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu tuân thủ"));
        Guard.input(c.getCode().equals(req.code()), "Không đổi mã của yêu cầu tuân thủ đã tạo");
        applyRequirement(c, req);
        return toRequirement(c);
    }

    private void applyRequirement(ComplianceRequirement c, RequirementRequest req) {
        if (req.originCountry() != null) {
            Guard.input(Countries.isSupported(req.originCountry()), "Quốc gia đi không được hỗ trợ");
        }
        if (req.destinationCountry() != null) {
            Guard.input(Countries.isSupported(req.destinationCountry()), "Quốc gia đến không được hỗ trợ");
        }
        if (req.effectiveTo() != null) {
            Guard.input(!req.effectiveTo().isBefore(req.effectiveFrom()), "Ngày hết hiệu lực phải sau ngày hiệu lực");
        }
        c.setCode(req.code());
        c.setOriginCountry(blankToNull(req.originCountry()));
        c.setDestinationCountry(blankToNull(req.destinationCountry()));
        c.setMovementType(req.movementType());
        c.setPurpose(req.purpose());
        c.setCategory(req.category());
        c.setDocumentType(req.documentType());
        c.setTitle(req.title());
        c.setDescription(req.description());
        c.setValidityDays(req.validityDays());
        c.setPerHorse(req.perHorse() == null || req.perHorse());
        c.setMandatory(req.mandatory() == null || req.mandatory());
        c.setEffectiveFrom(req.effectiveFrom());
        c.setEffectiveTo(req.effectiveTo());
        c.setActive(req.active() == null || req.active());
    }

    // ================================================================== mapping
    private ChecklistItemResponse toItem(ComplianceChecklistItem i) {
        ComplianceRequirement req = i.getRequirement();
        Document d = i.getDocument();
        return new ChecklistItemResponse(i.getId(), req.getId(), req.getCode(), req.getTitle(), req.getDescription(),
                req.getCategory(), req.getDocumentType(), Boolean.TRUE.equals(req.getMandatory()),
                req.getValidityDays(), i.getHorse() == null ? null : i.getHorse().getId(),
                i.getHorse() == null ? null : i.getHorse().getRegisteredName(), i.getStatus(),
                d == null ? null : d.getId(), d == null ? null : d.getFile().getOriginalFilename(),
                d == null ? null : d.getReviewStatus(), d == null ? null : d.getExpiryDate(), i.getReviewerNote(),
                i.getCheckedBy() == null ? null : i.getCheckedBy().getFullName(), i.getCheckedAt(), i.getDueDate());
    }

    public static RequirementResponse toRequirement(ComplianceRequirement c) {
        return new RequirementResponse(c.getId(), c.getCode(), c.getOriginCountry(), c.getDestinationCountry(),
                c.getMovementType(), c.getPurpose(), c.getCategory(), c.getDocumentType(), c.getTitle(),
                c.getDescription(), c.getValidityDays(), Boolean.TRUE.equals(c.getPerHorse()),
                Boolean.TRUE.equals(c.getMandatory()), c.getEffectiveFrom(), c.getEffectiveTo(),
                Boolean.TRUE.equals(c.getActive()));
    }

    private static String key(Long requirementId, Long horseId) {
        return requirementId + ":" + (horseId == null ? "-" : horseId);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static void requireSpecialist(UserPrincipal actor) {
        if (!actor.isSpecialist()) {
            throw new ForbiddenException("Chỉ Transport Specialist quản lý danh mục yêu cầu tuân thủ");
        }
    }

    /** Dùng trong RequestService: danh sách mục còn mở. */
    public List<ComplianceChecklistItem> openItemsOf(Long requestId) {
        List<ComplianceChecklistItem> out = new ArrayList<>();
        for (ComplianceChecklistItem i : items.findByRequestId(requestId)) {
            if (OPEN_STATUSES.contains(i.getStatus())) {
                out.add(i);
            }
        }
        return out;
    }
}
