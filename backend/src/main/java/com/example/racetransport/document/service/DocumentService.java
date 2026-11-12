package com.example.racetransport.document.service;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.common.util.Countries;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.compliance.service.ComplianceService;
import com.example.racetransport.document.dto.DocumentDtos.DocumentResponse;
import com.example.racetransport.document.dto.DocumentDtos.ReviewRequest;
import com.example.racetransport.document.entity.Document;
import com.example.racetransport.document.entity.DocumentReviewStatus;
import com.example.racetransport.document.entity.DocumentType;
import com.example.racetransport.document.entity.StoredFile;
import com.example.racetransport.document.repository.DocumentRepository;
import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.horse.service.HorseService;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.transportationrequest.service.RequestAccess;
import com.example.racetransport.transportationrequest.service.RequestWorkflow;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Tài liệu pháp lý / thú y / hải quan (Health/Compliance Documents).
 * Khách hàng tải lên; Transport Specialist rà soát APPROVED / REJECTED / NEEDS_UPDATE.
 */
@Service
public class DocumentService {

    /** Khách hàng chỉ được tải tài liệu khi yêu cầu còn ở các trạng thái này. */
    private static final EnumSet<RequestStatus> UPLOADABLE = EnumSet.of(RequestStatus.DRAFT, RequestStatus.SUBMITTED,
            RequestStatus.UNDER_DOCUMENT_REVIEW, RequestStatus.INFORMATION_REQUIRED, RequestStatus.COMPLIANCE_REVIEW,
            RequestStatus.PENDING_MANAGER_APPROVAL, RequestStatus.APPROVED, RequestStatus.QUOTE_SENT,
            RequestStatus.QUOTE_ACCEPTED, RequestStatus.READY_FOR_PLANNING, RequestStatus.TRIP_PLANNED);

    private final DocumentRepository documents;
    private final FileStorageService storage;
    private final RequestAccess requestAccess;
    private final RequestWorkflow workflow;
    private final RequestHorseRepository requestHorses;
    private final HorseService horseService;
    private final ComplianceService complianceService;
    private final NotificationService notifications;
    private final AppUserRepository users;
    private final Clock clock;

    public DocumentService(DocumentRepository documents, FileStorageService storage, RequestAccess requestAccess,
                           RequestWorkflow workflow, RequestHorseRepository requestHorses, HorseService horseService,
                           ComplianceService complianceService, NotificationService notifications,
                           AppUserRepository users, Clock clock) {
        this.documents = documents;
        this.storage = storage;
        this.requestAccess = requestAccess;
        this.workflow = workflow;
        this.requestHorses = requestHorses;
        this.horseService = horseService;
        this.complianceService = complianceService;
        this.notifications = notifications;
        this.users = users;
        this.clock = clock;
    }

    // ================================================================== upload
    @Transactional
    public DocumentResponse upload(UserPrincipal actor, MultipartFile file, DocumentType type, Long requestId,
                                   Long horseId, Long checklistItemId, String title, String documentNumber,
                                   String issuingCountry, LocalDate issueDate, LocalDate expiryDate) {
        if (!actor.isCustomer() && !actor.isSpecialist()) {
            throw new ForbiddenException("Chỉ khách hàng hoặc Transport Specialist được tải tài liệu hồ sơ");
        }
        Guard.input(type != null, "Cần chọn loại tài liệu");
        Guard.input(requestId != null || horseId != null, "Tài liệu phải gắn với yêu cầu hoặc ngựa");
        if (issuingCountry != null && !issuingCountry.isBlank()) {
            Guard.input(Countries.isSupported(issuingCountry), "Quốc gia cấp không được hỗ trợ");
        }
        if (issueDate != null) {
            Guard.input(!issueDate.isAfter(LocalDate.now(clock)), "Ngày cấp không được ở tương lai");
        }
        if (issueDate != null && expiryDate != null) {
            Guard.input(expiryDate.isAfter(issueDate), "Ngày hết hạn phải sau ngày cấp");
        }
        TransportationRequest request = null;
        if (requestId != null) {
            request = requestAccess.loadVisible(actor, requestId);
            Guard.state(!actor.isCustomer() || requestAccess.isOwner(actor, request),
                    "Bạn không sở hữu yêu cầu này");
            Guard.state(UPLOADABLE.contains(request.getStatus()), "Yêu cầu đã kết thúc, không thể tải thêm tài liệu");
        }
        Horse horse = null;
        if (horseId != null) {
            horse = actor.isCustomer() ? horseService.loadOwned(actor, horseId) : horseService.loadVisible(actor, horseId);
            if (request != null) {
                Guard.input(requestHorses.existsByRequestIdAndHorseId(request.getId(), horse.getId()),
                        "Ngựa không thuộc yêu cầu này");
            }
        }
        AppUser uploader = users.getReferenceById(actor.getId());
        StoredFile stored = storage.store(file, uploader);
        Document d = new Document();
        d.setDocumentType(type);
        d.setRequest(request);
        d.setHorse(horse);
        d.setFile(stored);
        d.setTitle(title == null || title.isBlank() ? type.getLabel() : title.trim());
        d.setDocumentNumber(documentNumber);
        d.setIssuingCountry(issuingCountry == null || issuingCountry.isBlank() ? null : issuingCountry);
        d.setIssueDate(issueDate);
        d.setExpiryDate(expiryDate);
        d.setReviewStatus(DocumentReviewStatus.PENDING_REVIEW);
        d.setUploadedBy(uploader);
        documents.save(d);
        if (request != null) {
            complianceService.attachDocument(request, d, checklistItemId);
            if (request.getStatus() != RequestStatus.DRAFT) {
                notifications.notifyRole(Role.TRANSPORT_SPECIALIST, NotificationType.ACTION_REQUIRED,
                        "Tài liệu mới chờ rà soát – " + request.getRequestNumber(),
                        type.getLabel() + (horse == null ? "" : " của " + horse.getRegisteredName()),
                        NotificationService.REF_DOCUMENT, d.getId());
            }
        }
        return toResponse(d);
    }

    // ================================================================== review
    @Transactional
    public DocumentResponse review(UserPrincipal actor, Long id, ReviewRequest req) {
        if (!actor.isSpecialist()) {
            throw new ForbiddenException("Chỉ Transport Specialist được rà soát tài liệu");
        }
        Document d = load(id);
        DocumentReviewStatus to = req.status();
        Guard.input(EnumSet.of(DocumentReviewStatus.APPROVED, DocumentReviewStatus.REJECTED,
                DocumentReviewStatus.NEEDS_UPDATE).contains(to), "Kết quả rà soát phải là Đạt, Không đạt hoặc Cần cập nhật");
        Guard.state(d.getReviewStatus() == DocumentReviewStatus.PENDING_REVIEW
                        || d.getReviewStatus() == DocumentReviewStatus.APPROVED,
                "Tài liệu ở trạng thái \"" + d.getReviewStatus().getLabel() + "\" không thể rà soát lại");
        if (to != DocumentReviewStatus.APPROVED) {
            Guard.input(req.note() != null && !req.note().isBlank(), "Cần ghi rõ lý do để khách hàng bổ sung");
        } else {
            validateForApproval(d);
        }
        d.setReviewStatus(to);
        d.setReviewNote(req.note());
        d.setReviewer(users.getReferenceById(actor.getId()));
        d.setReviewedAt(LocalDateTime.now(clock));

        TransportationRequest r = d.getRequest();
        if (r != null && r.getStatus() == RequestStatus.SUBMITTED) {
            workflow.transition(r, RequestStatus.UNDER_DOCUMENT_REVIEW, users.getReferenceById(actor.getId()),
                    "Bắt đầu rà soát tài liệu");
        }
        complianceService.onDocumentReviewed(d, users.getReferenceById(actor.getId()));
        if (to != DocumentReviewStatus.APPROVED) {
            AppUser customer = r != null ? r.getCustomer() : d.getHorse().getOwner();
            notifications.notify(customer, NotificationType.ACTION_REQUIRED,
                    d.getDocumentType().getLabel() + ": " + to.getLabel(),
                    req.note(), NotificationService.REF_DOCUMENT, d.getId());
        }
        return toResponse(d);
    }

    /** Tài liệu phải còn hiệu lực tới ngày khởi hành dự kiến. */
    private void validateForApproval(Document d) {
        LocalDate today = LocalDate.now(clock);
        if (d.getExpiryDate() != null) {
            Guard.state(!d.getExpiryDate().isBefore(today), "Tài liệu đã hết hạn, hãy chọn Cần cập nhật");
            if (d.getRequest() != null) {
                Guard.state(!d.getExpiryDate().isBefore(d.getRequest().getRequestedDepartureDate()),
                        "Tài liệu hết hạn trước ngày khởi hành dự kiến, hãy chọn Cần cập nhật");
            }
        }
    }

    // ================================================================== queries
    @Transactional(readOnly = true)
    public DocumentResponse get(UserPrincipal actor, Long id) {
        Document d = load(id);
        checkView(actor, d);
        return toResponse(d);
    }

    @Transactional(readOnly = true)
    public StoredFile fileOf(UserPrincipal actor, Long id) {
        Document d = load(id);
        checkView(actor, d);
        // open-in-view=false: nạp proxy LAZY ngay trong transaction để controller đọc được tên/loại tệp
        return Hibernate.unproxy(d.getFile()) instanceof StoredFile f ? f : d.getFile();
    }

    public Resource open(StoredFile file) {
        return storage.open(file);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listForRequest(UserPrincipal actor, Long requestId) {
        requestAccess.loadVisible(actor, requestId);
        return documents.findByRequestIdOrderByCreatedAtDesc(requestId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listForHorse(UserPrincipal actor, Long horseId) {
        if (actor.isDriverEscort()) {
            throw new ForbiddenException("Không có quyền xem hồ sơ tài liệu của ngựa");
        }
        horseService.loadVisible(actor, horseId);
        return documents.findByHorseIdOrderByCreatedAtDesc(horseId).stream().map(this::toResponse).toList();
    }

    /** Danh sách tài liệu cho Specialist (lọc theo trạng thái và nhóm loại tài liệu). */
    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> search(UserPrincipal actor, Collection<DocumentReviewStatus> statuses,
                                                 Collection<DocumentType> types, String q, int page, int size) {
        if (actor.isDriverEscort()) {
            throw new ForbiddenException("Không có quyền xem danh sách tài liệu");
        }
        Long customerId = actor.isCustomer() ? actor.getId() : null;
        var result = documents.search(
                statuses == null || statuses.isEmpty() ? EnumSet.allOf(DocumentReviewStatus.class) : statuses,
                types == null || types.isEmpty() ? EnumSet.allOf(DocumentType.class) : types,
                customerId, q == null || q.isBlank() ? null : q.trim(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.ASC, "createdAt")));
        return PageResponse.of(result, this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> expiring(int withinDays) {
        LocalDate today = LocalDate.now(clock);
        return documents.findExpiring(today, today.plusDays(Math.max(1, Math.min(withinDays, 365)))).stream()
                .map(this::toResponse).toList();
    }

    /** Đánh dấu EXPIRED các tài liệu đã duyệt nhưng quá hạn (Specialist gọi, hoặc khi xem dashboard). */
    @Transactional
    public int markExpired(UserPrincipal actor) {
        if (!actor.isSpecialist()) {
            throw new ForbiddenException("Chỉ Transport Specialist");
        }
        LocalDate today = LocalDate.now(clock);
        List<Document> expired = documents.findExpiredApproved(today);
        AppUser reviewer = users.getReferenceById(actor.getId());
        for (Document d : expired) {
            d.setReviewStatus(DocumentReviewStatus.EXPIRED);
            d.setReviewNote("Hết hạn ngày " + d.getExpiryDate());
            complianceService.onDocumentReviewed(d, reviewer);
        }
        return expired.size();
    }

    // ================================================================== helpers
    public Document load(Long id) {
        return documents.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy tài liệu"));
    }

    private void checkView(UserPrincipal actor, Document d) {
        if (actor.isCustomer()) {
            boolean own = (d.getRequest() != null && requestAccess.isOwner(actor, d.getRequest()))
                    || (d.getHorse() != null && d.getHorse().getOwner().getId().equals(actor.getId()));
            if (!own) {
                throw new ForbiddenException("Bạn không có quyền xem tài liệu này");
            }
        } else if (actor.isDriverEscort()) {
            if (d.getRequest() == null || !requestAccess.canView(actor, d.getRequest())) {
                throw new ForbiddenException("Bạn không có quyền xem tài liệu này");
            }
        }
    }

    public DocumentResponse toResponse(Document d) {
        LocalDate today = LocalDate.now(clock);
        Long days = d.getExpiryDate() == null ? null : ChronoUnit.DAYS.between(today, d.getExpiryDate());
        TransportationRequest r = d.getRequest();
        String customerName = r != null ? r.getCustomer().getFullName()
                : d.getHorse() != null ? d.getHorse().getOwner().getFullName() : null;
        return new DocumentResponse(d.getId(), d.getDocumentType(), d.getTitle(), d.getDocumentNumber(),
                d.getIssuingCountry(), d.getIssueDate(), d.getExpiryDate(), d.getReviewStatus(), d.getReviewNote(),
                d.getReviewer() == null ? null : d.getReviewer().getFullName(), d.getReviewedAt(),
                r == null ? null : r.getId(), r == null ? null : r.getRequestNumber(),
                d.getHorse() == null ? null : d.getHorse().getId(),
                d.getHorse() == null ? null : d.getHorse().getRegisteredName(), customerName,
                d.getFile().getOriginalFilename(), d.getFile().getContentType(), d.getFile().getSizeBytes(),
                d.getUploadedBy().getFullName(), d.getCreatedAt(), days, days != null && days < 0);
    }
}
