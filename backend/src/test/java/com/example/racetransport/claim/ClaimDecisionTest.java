package com.example.racetransport.claim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.racetransport.claim.dto.ClaimDtos.ApproveClaimRequest;
import com.example.racetransport.claim.dto.ClaimDtos.ClaimDetail;
import com.example.racetransport.claim.dto.ClaimDtos.CreateClaimRequest;
import com.example.racetransport.claim.dto.ClaimDtos.SettlementRequest;
import com.example.racetransport.claim.entity.Claim;
import com.example.racetransport.claim.entity.ClaimSettlement;
import com.example.racetransport.claim.entity.ClaimStatus;
import com.example.racetransport.claim.entity.ClaimType;
import com.example.racetransport.claim.repository.ClaimAssessmentRepository;
import com.example.racetransport.claim.repository.ClaimEvidenceRepository;
import com.example.racetransport.claim.repository.ClaimRepository;
import com.example.racetransport.claim.repository.ClaimSettlementRepository;
import com.example.racetransport.claim.service.ClaimService;
import com.example.racetransport.common.exception.BadRequestException;
import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.InvalidStateException;
import com.example.racetransport.common.repository.CodeSequenceRepository;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.incident.repository.IncidentRepository;
import com.example.racetransport.notification.repository.NotificationRepository;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.support.TestData;
import com.example.racetransport.transportationrequest.entity.RequestHorse;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.transportationrequest.repository.TransportationRequestRepository;
import com.example.racetransport.transportationrequest.service.RequestAccess;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Claim tách biệt Incident; số duyệt do Manager quyết định, không tự động bằng giá trị khai báo. */
class ClaimDecisionTest {

    private ClaimRepository claims;
    private ClaimEvidenceRepository evidences;
    private ClaimAssessmentRepository assessments;
    private ClaimSettlementRepository settlements;
    private TransportationRequestRepository requests;
    private RequestHorseRepository requestHorses;
    private ClaimService service;
    private AppUser owner;
    private AppUser manager;
    private TransportationRequest request;
    private Horse horse;
    private Claim claim;

    @BeforeEach
    void setUp() {
        Clock clock = TestData.clock();
        claims = mock(ClaimRepository.class);
        evidences = mock(ClaimEvidenceRepository.class);
        assessments = mock(ClaimAssessmentRepository.class);
        settlements = mock(ClaimSettlementRepository.class);
        requests = mock(TransportationRequestRepository.class);
        requestHorses = mock(RequestHorseRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        service = new ClaimService(claims, evidences, assessments, settlements, mock(IncidentRepository.class),
                mock(TransportationTripRepository.class), requestHorses,
                new RequestAccess(requests, mock(TripStaffAssignmentRepository.class)), null,
                new NotificationService(mock(NotificationRepository.class), users, clock), users,
                new CodeGenerator(mock(CodeSequenceRepository.class), clock), clock);
        owner = TestData.user(1, Role.CUSTOMER, "Owner");
        manager = TestData.user(3, Role.LOGISTICS_MANAGER, "Manager");
        when(users.getReferenceById(1L)).thenReturn(owner);
        when(users.getReferenceById(3L)).thenReturn(manager);
        request = TestData.request(152, owner, RequestStatus.COMPLETED);
        when(requests.findById(152L)).thenReturn(Optional.of(request));
        horse = TestData.horse(20, owner, "Midnight Star");
        RequestHorse rh = new RequestHorse();
        rh.setRequest(request);
        rh.setHorse(horse);
        rh.setDeclaredValue(new BigDecimal("180000.00"));
        when(requestHorses.findByRequestIdFetch(152L)).thenReturn(List.of(rh));

        claim = new Claim();
        claim.setId(14L);
        claim.setClaimNumber("CLM-2026-0014");
        claim.setRequest(request);
        claim.setCustomer(owner);
        claim.setHorse(horse);
        claim.setClaimType(ClaimType.INJURY_MEDICAL_EXPENSE);
        claim.setClaimedAmount(new BigDecimal("6500.00"));
        claim.setCurrency("USD");
        claim.setDescription("Chi phí điều trị");
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        when(claims.findById(14L)).thenReturn(Optional.of(claim));
    }

    @Test
    void partialApprovalUsesManagerAmountNotDeclaredValue() {
        when(assessments.countByClaimId(14L)).thenReturn(1L);
        when(evidences.countByClaimId(14L)).thenReturn(2L);
        ClaimDetail d = service.approve(TestData.principal(manager), 14L,
                new ApproveClaimRequest(new BigDecimal("5700"), "Loại trừ phí lưu chuồng"));
        assertEquals(ClaimStatus.PARTIALLY_APPROVED, d.status());
        assertTrue(TestData.same("5700", claim.getApprovedAmount()));
        assertTrue(TestData.same("180000", d.declaredValueReference()), "giá trị khai báo chỉ là tham chiếu");
    }

    @Test
    void approvedAmountCannotExceedClaimed() {
        when(assessments.countByClaimId(14L)).thenReturn(1L);
        when(evidences.countByClaimId(14L)).thenReturn(1L);
        assertThrows(BadRequestException.class, () -> service.approve(TestData.principal(manager), 14L,
                new ApproveClaimRequest(new BigDecimal("180000"), "Bồi thường toàn bộ giá trị ngựa")));
    }

    @Test
    void approvalNeedsAssessmentAndEvidence() {
        assertThrows(InvalidStateException.class, () -> service.approve(TestData.principal(manager), 14L,
                new ApproveClaimRequest(new BigDecimal("1000"), "x")));
    }

    @Test
    void customerCannotApprove() {
        assertThrows(ForbiddenException.class, () -> service.approve(TestData.principal(owner), 14L,
                new ApproveClaimRequest(new BigDecimal("1000"), "x")));
    }

    @Test
    void mortalityClaimCannotExceedDeclaredValue() {
        CreateClaimRequest req = new CreateClaimRequest(152L, null, null, 20L, ClaimType.MORTALITY,
                new BigDecimal("250000"), "Ngựa chết");
        assertThrows(BadRequestException.class, () -> service.create(TestData.principal(owner), req));
        verify(claims, never()).save(any(Claim.class));
    }

    @Test
    void settlementCannotExceedApprovedAndFullSettlementMarksPaid() {
        claim.setStatus(ClaimStatus.PARTIALLY_APPROVED);
        claim.setApprovedAmount(new BigDecimal("5700.00"));
        assertThrows(BadRequestException.class, () -> service.settle(TestData.principal(manager), 14L,
                new SettlementRequest(new BigDecimal("6000"), null, null)));
        service.settle(TestData.principal(manager), 14L, new SettlementRequest(new BigDecimal("5700"), null, null));
        assertEquals(ClaimStatus.PAID, claim.getStatus());
        verify(settlements).save(any(ClaimSettlement.class));
    }

    @Test
    void cannotSettleBeforeApproval() {
        assertThrows(InvalidStateException.class, () -> service.settle(TestData.principal(manager), 14L,
                new SettlementRequest(new BigDecimal("100"), null, null)));
    }
}
