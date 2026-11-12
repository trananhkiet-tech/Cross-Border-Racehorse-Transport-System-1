package com.example.racetransport.compliance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.InvalidStateException;
import com.example.racetransport.compliance.dto.ComplianceDtos.PatchChecklistItemRequest;
import com.example.racetransport.compliance.entity.ChecklistItemStatus;
import com.example.racetransport.compliance.entity.ComplianceCategory;
import com.example.racetransport.compliance.entity.ComplianceChecklistItem;
import com.example.racetransport.compliance.entity.ComplianceRequirement;
import com.example.racetransport.compliance.repository.ComplianceChecklistItemRepository;
import com.example.racetransport.compliance.repository.ComplianceRequirementRepository;
import com.example.racetransport.compliance.service.ComplianceService;
import com.example.racetransport.document.entity.Document;
import com.example.racetransport.document.entity.DocumentReviewStatus;
import com.example.racetransport.document.entity.DocumentType;
import com.example.racetransport.document.repository.DocumentRepository;
import com.example.racetransport.notification.repository.NotificationRepository;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.support.TestData;
import com.example.racetransport.transportationrequest.entity.RequestHorse;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.transportationrequest.repository.TransportationRequestRepository;
import com.example.racetransport.transportationrequest.service.RequestAccess;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checklist sinh theo tuyến/mục đích; Specialist không thể đánh dấu Đạt khi chưa có tài liệu được duyệt. */
class ComplianceChecklistTest {

    private ComplianceRequirementRepository requirements;
    private ComplianceChecklistItemRepository items;
    private RequestHorseRepository requestHorses;
    private ComplianceService service;
    private TransportationRequest request;
    private AppUser specialist;

    @BeforeEach
    void setUp() {
        Clock clock = TestData.clock();
        requirements = mock(ComplianceRequirementRepository.class);
        items = mock(ComplianceChecklistItemRepository.class);
        requestHorses = mock(RequestHorseRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        service = new ComplianceService(requirements, items, requestHorses, mock(DocumentRepository.class),
                new RequestAccess(mock(TransportationRequestRepository.class), mock(TripStaffAssignmentRepository.class)),
                new NotificationService(mock(NotificationRepository.class), users, clock), users, clock);
        AppUser customer = TestData.user(1, Role.CUSTOMER, "Owner");
        specialist = TestData.user(4, Role.TRANSPORT_SPECIALIST, "Specialist");
        when(users.getReferenceById(4L)).thenReturn(specialist);
        request = TestData.request(158, customer, RequestStatus.SUBMITTED);
    }

    private static ComplianceRequirement requirement(long id, String code, DocumentType type, boolean perHorse) {
        ComplianceRequirement r = new ComplianceRequirement();
        r.setId(id);
        r.setCode(code);
        r.setTitle(code);
        r.setCategory(ComplianceCategory.IDENTIFICATION);
        r.setDocumentType(type);
        r.setPerHorse(perHorse);
        r.setMandatory(true);
        return r;
    }

    @Test
    void generatesPerHorseAndPerRequestItems() {
        when(requirements.findApplicable("VN", "SG", request.getMovementType(), request.getPurpose(),
                request.getRequestedDepartureDate())).thenReturn(List.of(
                requirement(1, "GEN-PASSPORT", DocumentType.HORSE_PASSPORT, true),
                requirement(2, "SG-IMPORT", DocumentType.IMPORT_PERMIT, false)));
        RequestHorse a = new RequestHorse();
        a.setHorse(TestData.horse(10, request.getCustomer(), "Silver Arrow"));
        RequestHorse b = new RequestHorse();
        b.setHorse(TestData.horse(11, request.getCustomer(), "Golden Lotus"));
        when(requestHorses.findByRequestIdFetch(158L)).thenReturn(List.of(a, b));

        int created = service.generateChecklist(request, specialist);

        assertEquals(3, created);
        verify(items, times(3)).save(any(ComplianceChecklistItem.class));
    }

    @Test
    void cannotApproveItemWithoutApprovedDocument() {
        ComplianceChecklistItem item = new ComplianceChecklistItem();
        item.setId(50L);
        item.setRequest(request);
        item.setRequirement(requirement(1, "GEN-PASSPORT", DocumentType.HORSE_PASSPORT, true));
        item.setStatus(ChecklistItemStatus.MISSING);
        when(items.findById(50L)).thenReturn(Optional.of(item));
        assertThrows(InvalidStateException.class, () -> service.patchItem(TestData.principal(specialist), 50L,
                new PatchChecklistItemRequest(ChecklistItemStatus.APPROVED, null, null)));
        assertEquals(ChecklistItemStatus.MISSING, item.getStatus());
    }

    @Test
    void onlySpecialistCanPatchChecklist() {
        AppUser manager = TestData.user(3, Role.LOGISTICS_MANAGER, "Manager");
        assertThrows(ForbiddenException.class, () -> service.patchItem(TestData.principal(manager), 50L,
                new PatchChecklistItemRequest(ChecklistItemStatus.WAIVED, "Miễn trừ theo quy định demo", null)));
    }

    @Test
    void documentNeedsUpdatePropagatesToChecklist() {
        ComplianceChecklistItem item = new ComplianceChecklistItem();
        item.setId(51L);
        item.setRequest(request);
        item.setRequirement(requirement(3, "GEN-VACCINATION", DocumentType.VACCINATION_RECORD, true));
        item.setStatus(ChecklistItemStatus.SUBMITTED);
        Document doc = new Document();
        doc.setId(70L);
        doc.setRequest(request);
        doc.setDocumentType(DocumentType.VACCINATION_RECORD);
        doc.setReviewStatus(DocumentReviewStatus.NEEDS_UPDATE);
        doc.setReviewNote("Thiếu trang tiêm nhắc lại");
        doc.setIssueDate(LocalDate.of(2026, 9, 1));
        item.setDocument(doc);
        when(items.findByDocumentId(70L)).thenReturn(List.of(item));

        service.onDocumentReviewed(doc, specialist);

        assertEquals(ChecklistItemStatus.NEEDS_UPDATE, item.getStatus());
        assertEquals("Thiếu trang tiêm nhắc lại", item.getReviewerNote());
    }

    @Test
    void approvedButTooOldDocumentFailsValidityRule() {
        ComplianceRequirement health = requirement(4, "GEN-HEALTH-CERT", DocumentType.HEALTH_CERTIFICATE, true);
        health.setValidityDays(10);
        ComplianceChecklistItem item = new ComplianceChecklistItem();
        item.setId(52L);
        item.setRequest(request);
        item.setRequirement(health);
        item.setStatus(ChecklistItemStatus.SUBMITTED);
        Document doc = new Document();
        doc.setId(71L);
        doc.setRequest(request);
        doc.setDocumentType(DocumentType.HEALTH_CERTIFICATE);
        doc.setReviewStatus(DocumentReviewStatus.APPROVED);
        doc.setIssueDate(LocalDate.of(2026, 9, 1)); // > 10 ngày trước khởi hành 2026-10-20
        item.setDocument(doc);
        when(items.findByDocumentId(71L)).thenReturn(List.of(item));

        service.onDocumentReviewed(doc, specialist);

        assertEquals(ChecklistItemStatus.NEEDS_UPDATE, item.getStatus());
    }
}
