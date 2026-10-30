package com.example.racetransport.transportationrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.support.TestData;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.TransportationRequestRepository;
import com.example.racetransport.transportationrequest.service.RequestAccess;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Quyền xem/sửa Transportation Request theo role và quyền sở hữu. */
class RequestAccessTest {

    private TripStaffAssignmentRepository assignments;
    private RequestAccess access;
    private AppUser owner;

    @BeforeEach
    void setUp() {
        TransportationRequestRepository requests = mock(TransportationRequestRepository.class);
        assignments = mock(TripStaffAssignmentRepository.class);
        access = new RequestAccess(requests, assignments);
        owner = TestData.user(1, Role.CUSTOMER, "Owner");
        TransportationRequest r = TestData.request(158, owner, RequestStatus.SUBMITTED);
        when(requests.findById(158L)).thenReturn(Optional.of(r));
    }

    @Test
    void ownerCanViewAndEdit() {
        assertEquals(158L, access.loadVisible(TestData.principal(owner), 158L).getId());
        assertEquals(158L, access.loadOwned(TestData.principal(owner), 158L).getId());
    }

    @Test
    void anotherCustomerIsForbidden() {
        AppUser other = TestData.user(2, Role.CUSTOMER, "Other");
        assertThrows(ForbiddenException.class, () -> access.loadVisible(TestData.principal(other), 158L));
        assertThrows(ForbiddenException.class, () -> access.loadOwned(TestData.principal(other), 158L));
    }

    @Test
    void staffCanViewButNotActAsOwner() {
        AppUser specialist = TestData.user(3, Role.TRANSPORT_SPECIALIST, "Specialist");
        assertEquals(158L, access.loadVisible(TestData.principal(specialist), 158L).getId());
        assertThrows(ForbiddenException.class, () -> access.loadOwned(TestData.principal(specialist), 158L));
    }

    @Test
    void driverOnlyWhenAssigned() {
        AppUser driver = TestData.user(5, Role.DRIVER_ESCORT, "Driver");
        assertThrows(ForbiddenException.class, () -> access.loadVisible(TestData.principal(driver), 158L));
        when(assignments.existsByTripRequestIdAndStaffId(158L, 5L)).thenReturn(true);
        assertTrue(access.canView(TestData.principal(driver), access.load(158L)));
        assertFalse(access.isOwner(TestData.principal(driver), access.load(158L)));
    }
}
