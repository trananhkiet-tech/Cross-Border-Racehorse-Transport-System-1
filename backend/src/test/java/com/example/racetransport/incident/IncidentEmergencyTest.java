package com.example.racetransport.incident;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.InvalidStateException;
import com.example.racetransport.common.repository.CodeSequenceRepository;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.document.repository.AttachmentRepository;
import com.example.racetransport.document.service.AttachmentService;
import com.example.racetransport.handover.repository.HandoverRepository;
import com.example.racetransport.health.repository.ArrivalConditionCheckRepository;
import com.example.racetransport.health.repository.HealthObservationRepository;
import com.example.racetransport.health.repository.VeterinaryConsultationRepository;
import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.horse.repository.HorseRepository;
import com.example.racetransport.incident.dto.IncidentDtos.IncidentActionRequest;
import com.example.racetransport.incident.dto.IncidentDtos.IncidentDetail;
import com.example.racetransport.incident.dto.IncidentDtos.ReportIncidentRequest;
import com.example.racetransport.incident.entity.Incident;
import com.example.racetransport.incident.entity.IncidentActionType;
import com.example.racetransport.incident.entity.IncidentSeverity;
import com.example.racetransport.incident.entity.IncidentStatus;
import com.example.racetransport.incident.entity.IncidentType;
import com.example.racetransport.incident.entity.OperationalDecision;
import com.example.racetransport.incident.repository.IncidentActionRepository;
import com.example.racetransport.incident.repository.IncidentRepository;
import com.example.racetransport.incident.service.IncidentService;
import com.example.racetransport.notification.repository.NotificationRepository;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.route.entity.Route;
import com.example.racetransport.route.repository.RouteRepository;
import com.example.racetransport.route.repository.RouteStopRepository;
import com.example.racetransport.support.TestData;
import com.example.racetransport.tracking.repository.TrackingEventRepository;
import com.example.racetransport.tracking.repository.TripStatusHistoryRepository;
import com.example.racetransport.tracking.service.TripStatusService;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.trip.service.TripAccess;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Sự cố nghiêm trọng tạm dừng chuyến; chỉ Manager quyết định tiếp tục. Incident không tự tạo Claim. */
class IncidentEmergencyTest {

    private IncidentRepository incidents;
    private IncidentService service;
    private TransportationTrip trip;
    private AppUser escort;
    private AppUser manager;

    @BeforeEach
    void setUp() {
        Clock clock = TestData.clock();
        incidents = mock(IncidentRepository.class);
        TransportationTripRepository trips = mock(TransportationTripRepository.class);
        TripStaffAssignmentRepository assignments = mock(TripStaffAssignmentRepository.class);
        RequestHorseRepository requestHorses = mock(RequestHorseRepository.class);
        HorseRepository horses = mock(HorseRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        TripAccess tripAccess = new TripAccess(trips, assignments);
        AttachmentService attachments = new AttachmentService(mock(AttachmentRepository.class), null,
                mock(HealthObservationRepository.class), incidents, mock(HandoverRepository.class),
                mock(ArrivalConditionCheckRepository.class), mock(VeterinaryConsultationRepository.class),
                mock(TrackingEventRepository.class), tripAccess, users);
        service = new IncidentService(incidents, mock(IncidentActionRepository.class), mock(VeterinaryConsultationRepository.class),
                assignments, requestHorses, horses, mock(RouteRepository.class), mock(RouteStopRepository.class), tripAccess,
                new TripStatusService(mock(TripStatusHistoryRepository.class), mock(TrackingEventRepository.class), clock),
                attachments, new NotificationService(mock(NotificationRepository.class), users, clock), users,
                new CodeGenerator(mock(CodeSequenceRepository.class), clock), clock);

        AppUser customer = TestData.user(1, Role.CUSTOMER, "Customer");
        escort = TestData.user(6, Role.DRIVER_ESCORT, "Escort");
        manager = TestData.user(3, Role.LOGISTICS_MANAGER, "Manager");
        when(users.getReferenceById(6L)).thenReturn(escort);
        when(users.getReferenceById(3L)).thenReturn(manager);
        Route route = new Route();
        route.setId(1L);
        route.setName("TP.HCM → Bangkok");
        route.setDestinationCountry("TH");
        trip = new TransportationTrip();
        trip.setId(94L);
        trip.setTripNumber("TRIP-2026-0094");
        trip.setRequest(TestData.request(155, customer, RequestStatus.TRIP_PLANNED));
        trip.setRoute(route);
        trip.setStatus(TripStatus.AT_BORDER);
        trip.setOperationBlocked(false);
        trip.setCurrentLocation("Poipet");
        when(trips.findById(94L)).thenReturn(Optional.of(trip));
        when(assignments.existsByTripIdAndStaffId(94L, 6L)).thenReturn(true);
        Horse horse = TestData.horse(30, customer, "River Dancer");
        when(horses.findById(30L)).thenReturn(Optional.of(horse));
        when(requestHorses.existsByRequestIdAndHorseId(155L, 30L)).thenReturn(true);
        when(incidents.save(any(Incident.class))).thenAnswer(inv -> {
            Incident i = inv.getArgument(0);
            i.setId(53L);
            return i;
        });
    }

    private Incident reportCritical() {
        service.report(TestData.principal(escort), new ReportIncidentRequest(94L, 30L, IncidentType.HORSE_HEALTH,
                IncidentSeverity.CRITICAL, "Đau bụng", "Bỏ ăn, cào chân", "Poipet", null));
        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        org.mockito.Mockito.verify(incidents).save(captor.capture());
        Incident i = captor.getValue();
        when(incidents.findById(53L)).thenReturn(Optional.of(i));
        return i;
    }

    @Test
    void criticalIncidentBlocksTrip() {
        Incident i = reportCritical();
        assertEquals(IncidentStatus.OPEN, i.getStatus());
        assertEquals(TripStatus.DELAYED, trip.getStatus());
        assertTrue(trip.getOperationBlocked());
    }

    @Test
    void driverCannotDecideToContinue() {
        reportCritical();
        assertThrows(ForbiddenException.class, () -> service.addAction(TestData.principal(escort), 53L,
                new IncidentActionRequest(IncidentActionType.DECISION, "Đi tiếp", OperationalDecision.CONTINUE, null, null, null, null)));
        assertTrue(trip.getOperationBlocked());
    }

    @Test
    void criticalIncidentCannotBeResolvedWithoutDecision() {
        reportCritical();
        assertThrows(InvalidStateException.class,
                () -> service.changeStatus(TestData.principal(manager), 53L, IncidentStatus.RESOLVED, "Xong"));
    }

    @Test
    void managerDecisionContinueUnblocksTrip() {
        Incident i = reportCritical();
        IncidentDetail d = service.addAction(TestData.principal(manager), 53L,
                new IncidentActionRequest(IncidentActionType.DECISION, "Thú y xác nhận ổn định", OperationalDecision.CONTINUE,
                        null, null, null, null));
        assertEquals(OperationalDecision.CONTINUE, i.getDecision());
        assertEquals(IncidentStatus.RESOLVED, d.status());
        assertFalse(trip.getOperationBlocked());
        assertEquals(TripStatus.IN_TRANSIT, trip.getStatus());
    }

    @Test
    void stopDecisionKeepsTripBlocked() {
        reportCritical();
        service.addAction(TestData.principal(manager), 53L, new IncidentActionRequest(IncidentActionType.DECISION,
                "Dừng tại chỗ chờ thú y", OperationalDecision.STOP, null, null, null, null));
        assertTrue(trip.getOperationBlocked());
        assertEquals(TripStatus.DELAYED, trip.getStatus());
    }
}
