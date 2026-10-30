package com.example.racetransport.transportationrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.racetransport.common.exception.InvalidStateException;
import com.example.racetransport.support.TestData;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.RequestStatusHistory;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestStatusHistoryRepository;
import com.example.racetransport.transportationrequest.service.RequestWorkflow;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** State transitions hợp lệ / không hợp lệ của Transportation Request. */
class RequestWorkflowTest {

    private RequestStatusHistoryRepository history;
    private RequestWorkflow workflow;
    private AppUser customer;

    @BeforeEach
    void setUp() {
        history = mock(RequestStatusHistoryRepository.class);
        workflow = new RequestWorkflow(history, TestData.clock());
        customer = TestData.user(1, Role.CUSTOMER, "Tran Thu Ha");
    }

    @Test
    void submitFromDraftSetsSubmittedAtAndRecordsHistory() {
        TransportationRequest r = TestData.request(10, customer, RequestStatus.DRAFT);
        workflow.transition(r, RequestStatus.SUBMITTED, customer, "Gửi");
        assertEquals(RequestStatus.SUBMITTED, r.getStatus());
        assertNotNull(r.getSubmittedAt());
        ArgumentCaptor<RequestStatusHistory> captor = ArgumentCaptor.forClass(RequestStatusHistory.class);
        verify(history).save(captor.capture());
        assertEquals(RequestStatus.DRAFT, captor.getValue().getFromStatus());
        assertEquals(RequestStatus.SUBMITTED, captor.getValue().getToStatus());
    }

    @Test
    void cannotApproveDraftDirectly() {
        TransportationRequest r = TestData.request(11, customer, RequestStatus.DRAFT);
        assertThrows(InvalidStateException.class, () -> workflow.transition(r, RequestStatus.APPROVED, customer, null));
        assertEquals(RequestStatus.DRAFT, r.getStatus());
        verify(history, never()).save(any());
    }

    @Test
    void terminalStatesHaveNoTransitions() {
        for (RequestStatus to : RequestStatus.values()) {
            assertFalse(RequestWorkflow.canTransition(RequestStatus.COMPLETED, to));
            assertFalse(RequestWorkflow.canTransition(RequestStatus.REJECTED, to));
            assertFalse(RequestWorkflow.canTransition(RequestStatus.CANCELLED, to));
        }
    }

    @Test
    void reviewPipelineIsOrdered() {
        assertTrue(RequestWorkflow.canTransition(RequestStatus.SUBMITTED, RequestStatus.UNDER_DOCUMENT_REVIEW));
        assertTrue(RequestWorkflow.canTransition(RequestStatus.UNDER_DOCUMENT_REVIEW, RequestStatus.COMPLIANCE_REVIEW));
        assertTrue(RequestWorkflow.canTransition(RequestStatus.COMPLIANCE_REVIEW, RequestStatus.PENDING_MANAGER_APPROVAL));
        assertTrue(RequestWorkflow.canTransition(RequestStatus.PENDING_MANAGER_APPROVAL, RequestStatus.APPROVED));
        assertFalse(RequestWorkflow.canTransition(RequestStatus.SUBMITTED, RequestStatus.PENDING_MANAGER_APPROVAL));
        assertFalse(RequestWorkflow.canTransition(RequestStatus.APPROVED, RequestStatus.QUOTE_ACCEPTED));
        assertTrue(RequestWorkflow.canTransition(RequestStatus.INFORMATION_REQUIRED, RequestStatus.SUBMITTED));
    }

    @Test
    void cannotPlanTripBeforePayment() {
        assertFalse(RequestWorkflow.canTransition(RequestStatus.QUOTE_ACCEPTED, RequestStatus.TRIP_PLANNED));
        assertTrue(RequestWorkflow.canTransition(RequestStatus.QUOTE_ACCEPTED, RequestStatus.READY_FOR_PLANNING));
    }
}
