package com.example.racetransport.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.racetransport.common.exception.BadRequestException;
import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.InvalidStateException;
import com.example.racetransport.handover.repository.HandoverRepository;
import com.example.racetransport.health.repository.ArrivalConditionCheckRepository;
import com.example.racetransport.health.repository.HealthObservationRepository;
import com.example.racetransport.health.repository.PreTransportHealthCheckRepository;
import com.example.racetransport.incident.repository.IncidentActionRepository;
import com.example.racetransport.incident.repository.IncidentRepository;
import com.example.racetransport.notification.repository.NotificationRepository;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.route.repository.RouteStopRepository;
import com.example.racetransport.support.TestData;
import com.example.racetransport.tracking.repository.TrackingEventRepository;
import com.example.racetransport.tracking.repository.TripStatusHistoryRepository;
import com.example.racetransport.tracking.service.TripStatusService;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.trip.dto.TripDtos.StatusUpdateRequest;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.trip.service.TripAccess;
import com.example.racetransport.trip.service.TripExecutionService;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Driver chỉ thao tác chuyến được giao; chuyến bị tạm dừng do sự cố không thể tự tiếp tục. */
class TripExecutionGuardTest {

    private TripStaffAssignmentRepository assignments;
    private TripExecutionService service;
    private TransportationTrip trip;
    private AppUser driver;

    @BeforeEach
    void setUp() {
        Clock clock = TestData.clock();
        TransportationTripRepository trips = mock(TransportationTripRepository.class);
        assignments = mock(TripStaffAssignmentRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        service = new TripExecutionService(new TripAccess(trips, assignments), null,
                new TripStatusService(mock(TripStatusHistoryRepository.class), mock(TrackingEventRepository.class), clock),
                assignments, mock(RequestHorseRepository.class), mock(PreTransportHealthCheckRepository.class),
                mock(ArrivalConditionCheckRepository.class), mock(HealthObservationRepository.class),
                mock(IncidentRepository.class), mock(IncidentActionRepository.class), mock(HandoverRepository.class),
                mock(RouteStopRepository.class), mock(TrackingEventRepository.class), mock(TripStatusHistoryRepository.class),
                new NotificationService(mock(NotificationRepository.class), users, clock), users, clock);
        driver = TestData.user(5, Role.DRIVER_ESCORT, "Driver");
        trip = new TransportationTrip();
        trip.setId(94L);
        trip.setTripNumber("TRIP-2026-0094");
        trip.setRequest(TestData.request(155, TestData.user(1, Role.CUSTOMER, "C"), RequestStatus.TRIP_PLANNED));
        trip.setStatus(TripStatus.DELAYED);
        trip.setOperationBlocked(true);
        when(trips.findById(94L)).thenReturn(Optional.of(trip));
    }

    @Test
    void unassignedDriverCannotUpdateTrip() {
        assertThrows(ForbiddenException.class, () -> service.updateStatus(TestData.principal(driver), 94L,
                new StatusUpdateRequest(TripStatus.IN_TRANSIT, null, null, null, null, null)));
        assertThrows(ForbiddenException.class, () -> service.start(TestData.principal(driver), 94L));
    }

    @Test
    void blockedTripCannotContinue() {
        when(assignments.existsByTripIdAndStaffId(94L, 5L)).thenReturn(true);
        assertThrows(InvalidStateException.class, () -> service.updateStatus(TestData.principal(driver), 94L,
                new StatusUpdateRequest(TripStatus.IN_TRANSIT, "Đi tiếp", null, null, null, null)));
        assertEquals(TripStatus.DELAYED, trip.getStatus());
    }

    @Test
    void handoverStatusesAreSystemControlled() {
        when(assignments.existsByTripIdAndStaffId(94L, 5L)).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.updateStatus(TestData.principal(driver), 94L,
                new StatusUpdateRequest(TripStatus.DELIVERED, null, null, null, null, null)));
    }

    @Test
    void startRequiresReadyForDeparture() {
        when(assignments.existsByTripIdAndStaffId(94L, 5L)).thenReturn(true);
        trip.setStatus(TripStatus.SCHEDULED);
        trip.setOperationBlocked(false);
        assertThrows(InvalidStateException.class, () -> service.start(TestData.principal(driver), 94L));
    }
}
