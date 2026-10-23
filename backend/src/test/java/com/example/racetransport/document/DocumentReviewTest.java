package com.example.racetransport.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.racetransport.common.exception.BadRequestException;
import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.InvalidStateException;
import com.example.racetransport.compliance.repository.ComplianceChecklistItemRepository;
import com.example.racetransport.compliance.repository.ComplianceRequirementRepository;
import com.example.racetransport.compliance.service.ComplianceService;
import com.example.racetransport.document.dto.DocumentDtos.DocumentResponse;
import com.example.racetransport.document.dto.DocumentDtos.ReviewRequest;
import com.example.racetransport.document.entity.Document;
import com.example.racetransport.document.entity.DocumentReviewStatus;
import com.example.racetransport.document.entity.DocumentType;
import com.example.racetransport.document.entity.StoredFile;
import com.example.racetransport.document.repository.DocumentRepository;
import com.example.racetransport.document.service.DocumentService;
import com.example.racetransport.notification.repository.NotificationRepository;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.support.TestData;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.transportationrequest.repository.RequestStatusHistoryRepository;
import com.example.racetransport.transportationrequest.repository.TransportationRequestRepository;
import com.example.racetransport.transportationrequest.service.RequestAccess;
import com.example.racetransport.transportationrequest.service.RequestWorkflow;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Specialist rà soát tài liệu: APPROVED / REJECTED / NEEDS_UPDATE với quy tắc ghi chú và hạn hiệu lực. */
class DocumentReviewTest {

    private DocumentService service;
    private Document doc;
    private TransportationRequest request;
    private AppUser specialist;

    @BeforeEach
    void setUp() {
        Clock clock = TestData.clock();
        DocumentRepository documents = mock(DocumentRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        TransportationRequestRepository requests = mock(TransportationRequestRepository.class);
        RequestAccess access = new RequestAccess(requests, mock(TripStaffAssignmentRepository.class));
        NotificationService notifications = new NotificationService(mock(NotificationRepository.class), users, clock);
        ComplianceService compliance = new ComplianceService(mock(ComplianceRequirementRepository.class),
                mock(ComplianceChecklistItemRepository.class), mock(RequestHorseRepository.class), documents, access,
                notifications, users, clock);
        service = new DocumentService(documents, null, access, new RequestWorkflow(mock(RequestStatusHistoryRepository.class), clock),
                mock(RequestHorseRepository.class), null, compliance, notifications, users, clock);
        specialist = TestData.user(4, Role.TRANSPORT_SPECIALIST, "Specialist");
        when(users.getReferenceById(4L)).thenReturn(specialist);
        AppUser customer = TestData.user(1, Role.CUSTOMER, "Owner");
        request = TestData.request(158, customer, RequestStatus.SUBMITTED);
        StoredFile f = new StoredFile();
        f.setId(1L);
        f.setOriginalFilename("passport.pdf");
        f.setContentType("application/pdf");
        f.setSizeBytes(100L);
        doc = new Document();
        doc.setId(70L);
        doc.setRequest(request);
        doc.setDocumentType(DocumentType.HORSE_PASSPORT);
        doc.setFile(f);
        doc.setReviewStatus(DocumentReviewStatus.PENDING_REVIEW);
        doc.setUploadedBy(customer);
        doc.setExpiryDate(LocalDate.of(2027, 10, 1));
        when(documents.findById(70L)).thenReturn(Optional.of(doc));
    }

    @Test
    void approveMovesRequestIntoDocumentReview() {
        DocumentResponse res = service.review(TestData.principal(specialist), 70L,
                new ReviewRequest(DocumentReviewStatus.APPROVED, null));
        assertEquals(DocumentReviewStatus.APPROVED, res.reviewStatus());
        assertEquals(RequestStatus.UNDER_DOCUMENT_REVIEW, request.getStatus());
        assertEquals("Specialist", res.reviewerName());
    }

    @Test
    void needsUpdateRequiresNote() {
        assertThrows(BadRequestException.class, () -> service.review(TestData.principal(specialist), 70L,
                new ReviewRequest(DocumentReviewStatus.NEEDS_UPDATE, " ")));
        assertEquals(DocumentReviewStatus.PENDING_REVIEW, doc.getReviewStatus());
    }

    @Test
    void cannotApproveDocumentExpiringBeforeDeparture() {
        doc.setExpiryDate(LocalDate.of(2026, 10, 15)); // khởi hành 2026-10-20
        assertThrows(InvalidStateException.class, () -> service.review(TestData.principal(specialist), 70L,
                new ReviewRequest(DocumentReviewStatus.APPROVED, null)));
    }

    @Test
    void customerCannotReview() {
        AppUser customer = request.getCustomer();
        assertThrows(ForbiddenException.class, () -> service.review(TestData.principal(customer), 70L,
                new ReviewRequest(DocumentReviewStatus.APPROVED, null)));
    }

    @Test
    void reviewStatusMustBeAFinalDecision() {
        assertThrows(BadRequestException.class, () -> service.review(TestData.principal(specialist), 70L,
                new ReviewRequest(DocumentReviewStatus.EXPIRED, "x")));
    }
}
