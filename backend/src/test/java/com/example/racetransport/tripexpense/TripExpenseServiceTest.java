package com.example.racetransport.tripexpense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.racetransport.common.exception.BadRequestException;
import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.InvalidStateException;
import com.example.racetransport.notification.repository.NotificationRepository;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.support.TestData;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.trip.service.TripAccess;
import com.example.racetransport.tripexpense.dto.ExpenseDtos.ExpenseRequest;
import com.example.racetransport.tripexpense.dto.ExpenseDtos.ExpenseResponse;
import com.example.racetransport.tripexpense.dto.ExpenseDtos.ReviewRequest;
import com.example.racetransport.tripexpense.entity.ExpenseCategory;
import com.example.racetransport.tripexpense.entity.ExpenseStatus;
import com.example.racetransport.tripexpense.entity.TripExpense;
import com.example.racetransport.tripexpense.repository.TripExpenseRepository;
import com.example.racetransport.tripexpense.service.TripExpenseService;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TripExpenseServiceTest {

    private TripExpenseRepository expenses;
    private TripStaffAssignmentRepository assignments;
    private TripExpenseService service;
    private TransportationTrip trip;
    private AppUser driver;
    private AppUser manager;

    @BeforeEach
    void setUp() {
        Clock clock = TestData.clock();
        expenses = mock(TripExpenseRepository.class);
        assignments = mock(TripStaffAssignmentRepository.class);
        TransportationTripRepository trips = mock(TransportationTripRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        service = new TripExpenseService(expenses, new TripAccess(trips, assignments), trips, users,
                new NotificationService(mock(NotificationRepository.class), users, clock), clock);
        driver = TestData.user(9, Role.DRIVER_ESCORT, "Tài xế");
        manager = TestData.user(3, Role.LOGISTICS_MANAGER, "Manager");
        trip = new TransportationTrip();
        trip.setId(5L);
        trip.setTripNumber("TRP-2026-0005");
        trip.setStatus(TripStatus.IN_TRANSIT);
        when(trips.findById(5L)).thenReturn(Optional.of(trip));
        when(users.findById(9L)).thenReturn(Optional.of(driver));
        when(expenses.save(any(TripExpense.class))).thenAnswer(i -> i.getArgument(0));
    }

    private ExpenseRequest fuel() {
        return new ExpenseRequest(ExpenseCategory.FUEL, new BigDecimal("45.50"), "Đổ dầu");
    }

    @Test
    void assignedDriverRecordsExpenseAsPending() {
        when(assignments.existsByTripIdAndStaffId(5L, 9L)).thenReturn(true);
        ExpenseResponse r = service.create(TestData.principal(driver), 5L, fuel());
        assertEquals(ExpenseStatus.PENDING, r.status());
        assertEquals(new BigDecimal("45.50"), r.amount());
    }

    @Test
    void unassignedDriverIsRejected() {
        assertThrows(ForbiddenException.class, () -> service.create(TestData.principal(driver), 5L, fuel()));
    }

    @Test
    void cancelledTripRejectsNewExpense() {
        when(assignments.existsByTripIdAndStaffId(5L, 9L)).thenReturn(true);
        trip.setStatus(TripStatus.CANCELLED);
        assertThrows(InvalidStateException.class, () -> service.create(TestData.principal(driver), 5L, fuel()));
    }

    @Test
    void rejectionNeedsReasonAndFinalStatusIsLocked() {
        TripExpense e = new TripExpense();
        e.setTripId(5L);
        e.setStaffId(9L);
        e.setCategory(ExpenseCategory.TOLL);
        e.setAmount(new BigDecimal("10"));
        e.setCreatedAt(TestData.NOW);
        when(expenses.findById(1L)).thenReturn(Optional.of(e));
        assertThrows(BadRequestException.class,
                () -> service.review(TestData.principal(manager), 1L, new ReviewRequest(false, " ")));
        assertEquals(ExpenseStatus.APPROVED,
                service.review(TestData.principal(manager), 1L, new ReviewRequest(true, null)).status());
        assertThrows(InvalidStateException.class,
                () -> service.review(TestData.principal(manager), 1L, new ReviewRequest(false, "muộn")));
    }
}
