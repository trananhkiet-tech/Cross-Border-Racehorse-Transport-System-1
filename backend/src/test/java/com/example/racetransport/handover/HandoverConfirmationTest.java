package com.example.racetransport.handover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.racetransport.common.exception.BadRequestException;
import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.InvalidStateException;
import com.example.racetransport.common.repository.CodeSequenceRepository;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.document.repository.AttachmentRepository;
import com.example.racetransport.document.service.AttachmentService;
import com.example.racetransport.fleet.entity.Vehicle;
import com.example.racetransport.fleet.entity.VehicleStatus;
import com.example.racetransport.handover.dto.HandoverDtos.ConfirmHandoverRequest;
import com.example.racetransport.handover.entity.Handover;
import com.example.racetransport.handover.entity.HandoverStatus;
import com.example.racetransport.handover.repository.HandoverRepository;
import com.example.racetransport.handover.service.HandoverService;
import com.example.racetransport.health.entity.HorseCondition;
import com.example.racetransport.health.repository.ArrivalConditionCheckRepository;
import com.example.racetransport.health.repository.HealthObservationRepository;
import com.example.racetransport.health.repository.VeterinaryConsultationRepository;
import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.horse.entity.HorseStatus;
import com.example.racetransport.horse.repository.HorseRepository;
import com.example.racetransport.incident.repository.IncidentRepository;
import com.example.racetransport.notification.repository.NotificationRepository;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.route.entity.Route;
import com.example.racetransport.support.TestData;
import com.example.racetransport.tracking.repository.TrackingEventRepository;
import com.example.racetransport.tracking.repository.TripStatusHistoryRepository;
import com.example.racetransport.tracking.service.TripStatusService;
import com.example.racetransport.transportationrequest.entity.RequestHorse;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.transportationrequest.repository.RequestStatusHistoryRepository;
import com.example.racetransport.transportationrequest.service.RequestWorkflow;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.trip.service.TripAccess;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Handover chỉ được xác nhận khi trạng thái phù hợp; xác nhận đủ ngựa → chuyến DELIVERED, yêu cầu COMPLETED. */
class HandoverConfirmationTest {

    private HandoverRepository handovers;
    private HandoverService service;
    private AppUser owner;
    private TransportationRequest request;
    private TransportationTrip trip;
    private Handover handover;
    private Horse horse;

    @BeforeEach
    void setUp() {
        Clock clock = TestData.clock();
        handovers = mock(HandoverRepository.class);
        TransportationTripRepository trips = mock(TransportationTripRepository.class);
        TripStaffAssignmentRepository assignments = mock(TripStaffAssignmentRepository.class);
        RequestHorseRepository requestHorses = mock(RequestHorseRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        TripAccess tripAccess = new TripAccess(trips, assignments);
        AttachmentService attachments = new AttachmentService(mock(AttachmentRepository.class), null,
                mock(HealthObservationRepository.class), mock(IncidentRepository.class), handovers,
                mock(ArrivalConditionCheckRepository.class), mock(VeterinaryConsultationRepository.class),
                mock(TrackingEventRepository.class), tripAccess, users);
        service = new HandoverService(handovers, mock(ArrivalConditionCheckRepository.class), mock(HorseRepository.class),
                requestHorses, trips, assignments, tripAccess,
                new TripStatusService(mock(TripStatusHistoryRepository.class), mock(TrackingEventRepository.class), clock),
                new RequestWorkflow(mock(RequestStatusHistoryRepository.class), clock), attachments,
                new NotificationService(mock(NotificationRepository.class), users, clock), users,
                new CodeGenerator(mock(CodeSequenceRepository.class), clock), clock);

        owner = TestData.user(1, Role.CUSTOMER, "Owner");
        when(users.getReferenceById(1L)).thenReturn(owner);
        request = TestData.request(152, owner, RequestStatus.TRIP_PLANNED);
        horse = TestData.horse(20, owner, "Midnight Star");
        horse.setStatus(HorseStatus.IN_TRANSIT);
        Route route = new Route();
        route.setId(1L);
        route.setName("TP.HCM → Bangkok");
        route.setDestinationLocation("Bangkok");
        Vehicle vehicle = new Vehicle();
        vehicle.setId(7L);
        vehicle.setStatus(VehicleStatus.IN_USE);
        trip = new TransportationTrip();
        trip.setId(87L);
        trip.setTripNumber("TRIP-2026-0087");
        trip.setRequest(request);
        trip.setRoute(route);
        trip.setVehicle(vehicle);
        trip.setStatus(TripStatus.HANDOVER_PENDING);
        handover = new Handover();
        handover.setId(5L);
        handover.setHandoverNumber("HO-2026-0005");
        handover.setTrip(trip);
        handover.setHorse(horse);
        handover.setStatus(HandoverStatus.PENDING_CONFIRMATION);
        handover.setHandoverTime(LocalDateTime.of(2026, 10, 1, 8, 0));
        handover.setReceiverName("Somsak");
        handover.setHorseCondition(HorseCondition.GOOD);
        handover.setCreatedBy(TestData.user(5, Role.DRIVER_ESCORT, "Escort"));
        when(handovers.findById(5L)).thenReturn(Optional.of(handover));
        RequestHorse rh = new RequestHorse();
        rh.setHorse(horse);
        when(requestHorses.findByRequestIdFetch(152L)).thenReturn(List.of(rh));
        when(handovers.countByTripIdAndStatus(eq(87L), any())).thenReturn(1L);
        when(trips.findByVehicleIdOrderByPlannedDepartureDesc(7L)).thenReturn(List.of(trip));
        when(trips.findByRequestIdOrderByPlannedDepartureAsc(152L)).thenReturn(List.of(trip));
    }

    @Test
    void confirmingAllHorsesDeliversTripAndCompletesRequest() {
        service.confirm(TestData.principal(owner), 5L, new ConfirmHandoverRequest(true, "Đã nhận"));
        assertEquals(HandoverStatus.CONFIRMED, handover.getStatus());
        assertEquals(TripStatus.DELIVERED, trip.getStatus());
        assertEquals(RequestStatus.COMPLETED, request.getStatus());
        assertEquals(VehicleStatus.AVAILABLE, trip.getVehicle().getStatus());
        assertEquals(HorseStatus.ACTIVE, horse.getStatus());
    }

    @Test
    void anotherCustomerCannotConfirm() {
        AppUser other = TestData.user(2, Role.CUSTOMER, "Other");
        assertThrows(ForbiddenException.class,
                () -> service.confirm(TestData.principal(other), 5L, new ConfirmHandoverRequest(true, null)));
    }

    @Test
    void alreadyConfirmedCannotBeConfirmedAgain() {
        handover.setStatus(HandoverStatus.CONFIRMED);
        assertThrows(InvalidStateException.class,
                () -> service.confirm(TestData.principal(owner), 5L, new ConfirmHandoverRequest(true, null)));
    }

    @Test
    void disputeRequiresCommentAndKeepsTripPending() {
        assertThrows(BadRequestException.class,
                () -> service.confirm(TestData.principal(owner), 5L, new ConfirmHandoverRequest(false, " ")));
        service.confirm(TestData.principal(owner), 5L, new ConfirmHandoverRequest(false, "Thiếu hộ chiếu ngựa"));
        assertEquals(HandoverStatus.DISPUTED, handover.getStatus());
        assertEquals(TripStatus.HANDOVER_PENDING, trip.getStatus());
    }
}
