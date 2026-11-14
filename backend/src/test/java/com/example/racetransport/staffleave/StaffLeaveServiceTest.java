package com.example.racetransport.staffleave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.racetransport.common.exception.BadRequestException;
import com.example.racetransport.common.exception.InvalidStateException;
import com.example.racetransport.notification.repository.NotificationRepository;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.staffleave.dto.LeaveDtos.LeaveRequest;
import com.example.racetransport.staffleave.dto.LeaveDtos.LeaveResponse;
import com.example.racetransport.staffleave.entity.LeaveStatus;
import com.example.racetransport.staffleave.entity.StaffLeave;
import com.example.racetransport.staffleave.repository.StaffLeaveRepository;
import com.example.racetransport.staffleave.service.StaffLeaveService;
import com.example.racetransport.support.TestData;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaffLeaveServiceTest {

    private StaffLeaveRepository leaves;
    private TripStaffAssignmentRepository assignments;
    private StaffLeaveService service;
    private AppUser coordinator;
    private final LocalDate today = TestData.NOW.toLocalDate();

    @BeforeEach
    void setUp() {
        Clock clock = TestData.clock();
        leaves = mock(StaffLeaveRepository.class);
        assignments = mock(TripStaffAssignmentRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        service = new StaffLeaveService(leaves, users, assignments,
                new NotificationService(mock(NotificationRepository.class), users, clock), clock);
        coordinator = TestData.user(5, Role.FLEET_ROUTE_COORDINATOR, "Điều phối");
        when(users.findById(9L)).thenReturn(Optional.of(TestData.user(9, Role.DRIVER_ESCORT, "Tài xế")));
        when(users.findById(1L)).thenReturn(Optional.of(TestData.user(1, Role.CUSTOMER, "Khách")));
        when(leaves.save(any(StaffLeave.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void createsLeaveForDriver() {
        LeaveResponse r = service.create(TestData.principal(coordinator),
                new LeaveRequest(9L, today.plusDays(2), today.plusDays(4), " Việc gia đình "));
        assertEquals(LeaveStatus.ACTIVE, r.status());
        assertEquals("Việc gia đình", r.reason());
    }

    @Test
    void rejectsReversedDates() {
        assertThrows(BadRequestException.class, () -> service.create(TestData.principal(coordinator),
                new LeaveRequest(9L, today.plusDays(4), today.plusDays(2), null)));
    }

    @Test
    void rejectsNonDriverStaff() {
        assertThrows(BadRequestException.class, () -> service.create(TestData.principal(coordinator),
                new LeaveRequest(1L, today.plusDays(1), today.plusDays(2), null)));
    }

    @Test
    void rejectsOverlapWithAnotherLeave() {
        when(leaves.existsOverlap(anyLong(), any(), any())).thenReturn(true);
        assertThrows(InvalidStateException.class, () -> service.create(TestData.principal(coordinator),
                new LeaveRequest(9L, today.plusDays(1), today.plusDays(2), null)));
    }

    @Test
    void rejectsWhenStaffHasAssignedTrip() {
        when(assignments.existsStaffOverlap(anyLong(), anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);
        assertThrows(InvalidStateException.class, () -> service.create(TestData.principal(coordinator),
                new LeaveRequest(9L, today.plusDays(1), today.plusDays(2), null)));
    }
}
