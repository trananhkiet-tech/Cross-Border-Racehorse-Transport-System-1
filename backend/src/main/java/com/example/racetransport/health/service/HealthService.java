package com.example.racetransport.health.service;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.document.entity.AttachmentRefType;
import com.example.racetransport.document.entity.StoredFile;
import com.example.racetransport.document.service.AttachmentService;
import com.example.racetransport.document.service.FileStorageService;
import com.example.racetransport.health.dto.HealthDtos.ArrivalCheckRequest;
import com.example.racetransport.health.dto.HealthDtos.ArrivalCheckResponse;
import com.example.racetransport.health.dto.HealthDtos.HorseHealthHistory;
import com.example.racetransport.health.dto.HealthDtos.ObservationRequest;
import com.example.racetransport.health.dto.HealthDtos.ObservationResponse;
import com.example.racetransport.health.dto.HealthDtos.PreTransportCheckRequest;
import com.example.racetransport.health.dto.HealthDtos.PreTransportCheckResponse;
import com.example.racetransport.health.dto.HealthDtos.VetConsultationRequest;
import com.example.racetransport.health.dto.HealthDtos.VetConsultationResponse;
import com.example.racetransport.health.entity.ArrivalConditionCheck;
import com.example.racetransport.health.entity.HealthObservation;
import com.example.racetransport.health.entity.HorseBehavior;
import com.example.racetransport.health.entity.HorseCondition;
import com.example.racetransport.health.entity.IntakeLevel;
import com.example.racetransport.health.entity.MovementCondition;
import com.example.racetransport.health.entity.ObservationSeverity;
import com.example.racetransport.health.entity.PreTransportHealthCheck;
import com.example.racetransport.health.entity.StressLevel;
import com.example.racetransport.health.entity.VeterinaryConsultation;
import com.example.racetransport.health.mapper.HealthMapper;
import com.example.racetransport.health.repository.ArrivalConditionCheckRepository;
import com.example.racetransport.health.repository.HealthObservationRepository;
import com.example.racetransport.health.repository.PreTransportHealthCheckRepository;
import com.example.racetransport.health.repository.VeterinaryConsultationRepository;
import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.horse.repository.HorseRepository;
import com.example.racetransport.horse.service.HorseService;
import com.example.racetransport.incident.entity.Incident;
import com.example.racetransport.incident.entity.IncidentAction;
import com.example.racetransport.incident.entity.IncidentActionType;
import com.example.racetransport.incident.entity.IncidentSeverity;
import com.example.racetransport.incident.entity.IncidentType;
import com.example.racetransport.incident.repository.IncidentActionRepository;
import com.example.racetransport.incident.repository.IncidentRepository;
import com.example.racetransport.incident.service.IncidentService;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.tracking.service.TripStateMachine;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.service.TripAccess;
import com.example.racetransport.trip.service.TripExecutionService;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import org.springframework.core.io.Resource;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Ba loại dữ liệu sức khỏe được tách biệt:
 * 1) Health/Compliance Documents → module document/compliance (Specialist);
 * 2) Health Observation / kiểm tra trước & khi đến → module này (Driver/Escort quan sát, không chẩn đoán);
 * 3) Veterinary Consultation → bản ghi tư vấn từ bác sĩ thú y bên ngoài.
 */
@Service
public class HealthService {

    public static final String DISCLAIMER = "Thông tin là quan sát của đội vận chuyển và tư vấn ghi nhận từ bác sĩ thú y "
            + "bên ngoài; hệ thống không chẩn đoán hay kê đơn.";

    private static final EnumSet<TripStatus> OBSERVABLE = EnumSet.of(TripStatus.PREPARING, TripStatus.READY_FOR_DEPARTURE,
            TripStatus.IN_TRANSIT, TripStatus.AT_CHECKPOINT, TripStatus.AT_BORDER, TripStatus.CUSTOMS_CLEARANCE,
            TripStatus.QUARANTINE, TripStatus.DELAYED, TripStatus.ARRIVED, TripStatus.HANDOVER_PENDING);

    private final PreTransportHealthCheckRepository preChecks;
    private final HealthObservationRepository observations;
    private final ArrivalConditionCheckRepository arrivalChecks;
    private final VeterinaryConsultationRepository consultations;
    private final IncidentRepository incidents;
    private final IncidentActionRepository incidentActions;
    private final HorseRepository horses;
    private final RequestHorseRepository requestHorses;
    private final HorseService horseService;
    private final TripAccess tripAccess;
    private final TripExecutionService tripExecution;
    private final IncidentService incidentService;
    private final AttachmentService attachmentService;
    private final FileStorageService storage;
    private final NotificationService notifications;
    private final AppUserRepository users;
    private final Clock clock;

    public HealthService(PreTransportHealthCheckRepository preChecks, HealthObservationRepository observations,
                         ArrivalConditionCheckRepository arrivalChecks, VeterinaryConsultationRepository consultations,
                         IncidentRepository incidents, IncidentActionRepository incidentActions, HorseRepository horses,
                         RequestHorseRepository requestHorses, HorseService horseService, TripAccess tripAccess,
                         TripExecutionService tripExecution, IncidentService incidentService,
                         AttachmentService attachmentService, FileStorageService storage,
                         NotificationService notifications, AppUserRepository users, Clock clock) {
        this.preChecks = preChecks;
        this.observations = observations;
        this.arrivalChecks = arrivalChecks;
        this.consultations = consultations;
        this.incidents = incidents;
        this.incidentActions = incidentActions;
        this.horses = horses;
        this.requestHorses = requestHorses;
        this.horseService = horseService;
        this.tripAccess = tripAccess;
        this.tripExecution = tripExecution;
        this.incidentService = incidentService;
        this.attachmentService = attachmentService;
        this.storage = storage;
        this.notifications = notifications;
        this.users = users;
        this.clock = clock;
    }

    // ================================================================== pre-transport check
    /** Ghi (hoặc cập nhật) kiểm tra trước khi xếp ngựa lên xe; tự chuyển chuyến sang PREPARING/READY. */
    @Transactional
    public PreTransportCheckResponse recordPreTransport(UserPrincipal actor, PreTransportCheckRequest req) {
        TransportationTrip t = tripAccess.loadAssigned(actor, req.tripId());
        Guard.state(TripStateMachine.PRE_DEPARTURE.contains(t.getStatus()), "Chỉ kiểm tra trước vận chuyển khi chuyến chưa khởi hành");
        Horse horse = horseOfTrip(t, req.horseId());
        AppUser user = users.getReferenceById(actor.getId());
        PreTransportHealthCheck c = preChecks.findByTripIdAndHorseId(t.getId(), horse.getId()).orElseGet(PreTransportHealthCheck::new);
        c.setTrip(t);
        c.setHorse(horse);
        c.setCheckedBy(user);
        c.setCheckedAt(LocalDateTime.now(clock));
        c.setTemperatureC(req.temperatureC());
        c.setGeneralCondition(req.generalCondition());
        c.setIdentityVerified(Boolean.TRUE.equals(req.identityVerified()));
        c.setDocumentsPresent(Boolean.TRUE.equals(req.documentsPresent()));
        c.setLoadingOk(Boolean.TRUE.equals(req.loadingOk()));
        boolean fit = Boolean.TRUE.equals(req.fitForTravelObserved());
        if (fit) {
            Guard.input(Boolean.TRUE.equals(req.identityVerified()), "Cần đối chiếu microchip/hộ chiếu trước khi xác nhận phù hợp");
            Guard.input(req.generalCondition() != HorseCondition.INJURED, "Ngựa có dấu hiệu bị thương không thể xác nhận phù hợp; hãy báo sự cố");
        }
        c.setFitForTravelObserved(fit);
        c.setNotes(req.notes());
        preChecks.save(c);
        tripExecution.refreshReadiness(t, user);
        if (!fit) {
            notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.WARNING,
                    "Ngựa chưa phù hợp vận chuyển – " + t.getTripNumber(),
                    horse.getRegisteredName() + ": " + (req.notes() == null ? req.generalCondition().getLabel() : req.notes()),
                    NotificationService.REF_TRIP, t.getId());
        }
        return HealthMapper.toPreCheck(c);
    }

    @Transactional(readOnly = true)
    public List<PreTransportCheckResponse> preChecksOf(UserPrincipal actor, Long tripId) {
        TransportationTrip t = tripAccess.loadVisible(actor, tripId);
        return preChecks.findByTripIdOrderByCheckedAtAsc(t.getId()).stream().map(HealthMapper::toPreCheck).toList();
    }

    // ================================================================== observation
    @Transactional
    public ObservationResponse recordObservation(UserPrincipal actor, ObservationRequest req) {
        TransportationTrip t = tripAccess.loadAssigned(actor, req.tripId());
        Guard.state(OBSERVABLE.contains(t.getStatus()), "Chuyến không ở giai đoạn ghi quan sát");
        Horse horse = horseOfTrip(t, req.horseId());
        AppUser user = users.getReferenceById(actor.getId());
        LocalDateTime now = LocalDateTime.now(clock);
        HealthObservation o = new HealthObservation();
        o.setTrip(t);
        o.setHorse(horse);
        o.setAuthor(user);
        o.setObservedAt(req.observedAt() == null || req.observedAt().isAfter(now) ? now : req.observedAt());
        o.setLocation(req.location() == null || req.location().isBlank() ? t.getCurrentLocation() : req.location());
        o.setAppetite(req.appetite());
        o.setWaterIntake(req.waterIntake());
        o.setBehavior(req.behavior());
        o.setStressLevel(req.stressLevel());
        o.setMovement(req.movement());
        o.setVisibleInjury(Boolean.TRUE.equals(req.visibleInjury()));
        if (Boolean.TRUE.equals(req.visibleInjury())) {
            Guard.input(req.injuryDescription() != null && !req.injuryDescription().isBlank(), "Mô tả vết thương quan sát được");
        }
        o.setInjuryDescription(req.injuryDescription());
        o.setTemperatureC(req.temperatureC());
        o.setNotes(req.notes());
        o.setSeverity(max(req.severity(), computeSeverity(req)));
        observations.save(o);

        if (Boolean.TRUE.equals(req.reportIncident())) {
            IncidentSeverity sev = o.getSeverity() == ObservationSeverity.URGENT ? IncidentSeverity.HIGH : IncidentSeverity.MEDIUM;
            Incident i = incidentService.createIncident(t, horse,
                    o.getVisibleInjury() ? IncidentType.HORSE_INJURY : IncidentType.HORSE_HEALTH, sev,
                    "Dấu hiệu bất thường: " + horse.getRegisteredName(),
                    describe(o), o.getLocation(), o.getObservedAt(), user);
            o.setIncident(i);
        } else if (o.getSeverity() == ObservationSeverity.URGENT) {
            String msg = horse.getRegisteredName() + " – " + describe(o)
                    + ". Đề nghị Driver/Escort báo sự cố và liên hệ bác sĩ thú y bên ngoài.";
            notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.CRITICAL,
                    "Quan sát khẩn cấp trên chuyến " + t.getTripNumber(), msg, NotificationService.REF_TRIP, t.getId());
            notifications.notifyRole(Role.TRANSPORT_SPECIALIST, NotificationType.WARNING,
                    "Quan sát khẩn cấp trên chuyến " + t.getTripNumber(), msg, NotificationService.REF_TRIP, t.getId());
        }
        return HealthMapper.toObservation(o, List.of());
    }

    /** Mức độ do backend suy ra từ các dấu hiệu quan sát (không phải chẩn đoán). */
    public static ObservationSeverity computeSeverity(ObservationRequest r) {
        boolean urgent = Boolean.TRUE.equals(r.visibleInjury())
                || r.movement() == MovementCondition.LAME || r.movement() == MovementCondition.REFUSES_TO_MOVE
                || (r.appetite() == IntakeLevel.NONE && r.waterIntake() == IntakeLevel.NONE)
                || ((r.behavior() == HorseBehavior.AGITATED || r.behavior() == HorseBehavior.LETHARGIC)
                && r.stressLevel() == StressLevel.HIGH)
                || (r.temperatureC() != null && (r.temperatureC().doubleValue() >= 39.5 || r.temperatureC().doubleValue() < 36.5));
        if (urgent) {
            return ObservationSeverity.URGENT;
        }
        boolean attention = r.appetite() != IntakeLevel.NORMAL || r.waterIntake() != IntakeLevel.NORMAL
                || r.stressLevel() == StressLevel.HIGH || r.behavior() == HorseBehavior.RESTLESS
                || r.behavior() == HorseBehavior.AGITATED || r.behavior() == HorseBehavior.LETHARGIC
                || r.movement() == MovementCondition.STIFF
                || (r.temperatureC() != null && r.temperatureC().doubleValue() >= 38.6);
        return attention ? ObservationSeverity.ATTENTION : ObservationSeverity.NORMAL;
    }

    private static ObservationSeverity max(ObservationSeverity a, ObservationSeverity b) {
        if (a == null) {
            return b;
        }
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    private static String describe(HealthObservation o) {
        return "Ăn: " + o.getAppetite().getLabel() + ", uống: " + o.getWaterIntake().getLabel() + ", hành vi: "
                + o.getBehavior().getLabel() + ", căng thẳng: " + o.getStressLevel().getLabel() + ", di chuyển: "
                + o.getMovement().getLabel() + (Boolean.TRUE.equals(o.getVisibleInjury()) ? ", vết thương: " + o.getInjuryDescription() : "")
                + (o.getNotes() == null ? "" : ". " + o.getNotes());
    }

    @Transactional(readOnly = true)
    public List<ObservationResponse> observationsOf(UserPrincipal actor, Long tripId) {
        TransportationTrip t = tripAccess.loadVisible(actor, tripId);
        return observations.findByTripIdOrderByObservedAtDesc(t.getId()).stream()
                .map(o -> HealthMapper.toObservation(o, attachmentService.refs(AttachmentRefType.HEALTH_OBSERVATION, o.getId())))
                .toList();
    }

    // ================================================================== arrival
    @Transactional
    public ArrivalCheckResponse recordArrival(UserPrincipal actor, ArrivalCheckRequest req) {
        TransportationTrip t = tripAccess.loadAssigned(actor, req.tripId());
        Guard.state(t.getStatus() == TripStatus.ARRIVED || t.getStatus() == TripStatus.HANDOVER_PENDING,
                "Chỉ kiểm tra khi đến sau khi chuyến đã đến nơi");
        Horse horse = horseOfTrip(t, req.horseId());
        ArrivalConditionCheck c = arrivalChecks.findByTripIdAndHorseId(t.getId(), horse.getId()).orElseGet(ArrivalConditionCheck::new);
        c.setTrip(t);
        c.setHorse(horse);
        c.setCheckedBy(users.getReferenceById(actor.getId()));
        c.setCheckedAt(LocalDateTime.now(clock));
        c.setHorseCondition(req.condition());
        c.setVisibleInjury(Boolean.TRUE.equals(req.visibleInjury()));
        c.setNotes(req.notes());
        arrivalChecks.save(c);
        if (req.condition() == HorseCondition.INJURED || req.condition() == HorseCondition.POOR) {
            notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.WARNING,
                    "Tình trạng khi đến: " + req.condition().getLabel() + " – " + horse.getRegisteredName(),
                    req.notes(), NotificationService.REF_TRIP, t.getId());
        }
        return HealthMapper.toArrival(c);
    }

    @Transactional(readOnly = true)
    public List<ArrivalCheckResponse> arrivalChecksOf(UserPrincipal actor, Long tripId) {
        TransportationTrip t = tripAccess.loadVisible(actor, tripId);
        return arrivalChecks.findByTripIdOrderByCheckedAtAsc(t.getId()).stream().map(HealthMapper::toArrival).toList();
    }

    // ================================================================== veterinary consultation
    @Transactional
    public VetConsultationResponse recordConsultation(UserPrincipal actor, VetConsultationRequest req) {
        Guard.input(req.incidentId() != null || req.tripId() != null, "Tư vấn phải gắn với sự cố hoặc chuyến");
        Incident incident = req.incidentId() == null ? null : incidents.findById(req.incidentId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sự cố"));
        TransportationTrip t = incident != null ? incident.getTrip() : tripAccess.load(req.tripId());
        boolean assigned = actor.isDriverEscort() && tripAccess.isAssigned(actor, t);
        if (!actor.isManager() && !actor.isSpecialist() && !assigned) {
            throw new ForbiddenException("Bạn không có quyền ghi nhận tư vấn thú y cho chuyến này");
        }
        Horse horse = null;
        if (req.horseId() != null) {
            horse = horseOfTrip(t, req.horseId());
        } else if (incident != null) {
            horse = incident.getHorse();
        }
        AppUser user = users.getReferenceById(actor.getId());
        LocalDateTime now = LocalDateTime.now(clock);
        VeterinaryConsultation v = new VeterinaryConsultation();
        v.setIncident(incident);
        v.setTrip(t);
        v.setHorse(horse);
        v.setVeterinarianName(req.veterinarianName().trim());
        v.setClinic(req.clinic());
        v.setContact(req.contact());
        v.setConsultedAt(req.consultedAt() == null || req.consultedAt().isAfter(now) ? now : req.consultedAt());
        v.setAdvice(req.advice().trim());
        v.setRecommendedAction(req.recommendedAction());
        v.setRecordedBy(user);
        consultations.save(v);
        if (incident != null) {
            IncidentAction a = new IncidentAction();
            a.setIncident(incident);
            a.setActionType(IncidentActionType.VET_ADVICE_RECORDED);
            a.setNote("Tư vấn của " + v.getVeterinarianName() + (v.getClinic() == null ? "" : " (" + v.getClinic() + ")")
                    + ": " + v.getAdvice() + (v.getRecommendedAction() == null ? ""
                    : ". Khuyến nghị: " + v.getRecommendedAction().getLabel()));
            a.setPerformedBy(user);
            a.setPerformedAt(now);
            a.setConsultation(v);
            incidentActions.save(a);
            notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.ACTION_REQUIRED,
                    "Đã có tư vấn thú y cho " + incident.getIncidentNumber(),
                    "Khuyến nghị: " + (v.getRecommendedAction() == null ? "xem chi tiết" : v.getRecommendedAction().getLabel())
                            + ". Vui lòng ghi quyết định vận hành.", NotificationService.REF_INCIDENT, incident.getId());
        }
        return HealthMapper.toVet(v);
    }

    @Transactional(readOnly = true)
    public VetConsultationResponse getConsultation(UserPrincipal actor, Long id) {
        VeterinaryConsultation v = loadConsultationVisible(actor, id);
        return HealthMapper.toVet(v);
    }

    @Transactional
    public VetConsultationResponse attachReport(UserPrincipal actor, Long id, MultipartFile file) {
        VeterinaryConsultation v = consultations.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy tư vấn"));
        TransportationTrip t = v.getTrip() != null ? v.getTrip() : v.getIncident().getTrip();
        boolean assigned = actor.isDriverEscort() && tripAccess.isAssigned(actor, t);
        if (!actor.isManager() && !actor.isSpecialist() && !assigned) {
            throw new ForbiddenException("Bạn không có quyền đính kèm báo cáo thú y");
        }
        StoredFile f = storage.store(file, users.getReferenceById(actor.getId()));
        v.setReportFile(f);
        return HealthMapper.toVet(v);
    }

    @Transactional(readOnly = true)
    public StoredFile reportFile(UserPrincipal actor, Long id) {
        VeterinaryConsultation v = loadConsultationVisible(actor, id);
        Guard.state(v.getReportFile() != null, "Tư vấn chưa có tệp báo cáo");
        // open-in-view=false: nạp proxy LAZY ngay trong transaction để controller đọc được tên/loại tệp
        return Hibernate.unproxy(v.getReportFile()) instanceof StoredFile f ? f : v.getReportFile();
    }

    public Resource open(StoredFile f) {
        return storage.open(f);
    }

    private VeterinaryConsultation loadConsultationVisible(UserPrincipal actor, Long id) {
        VeterinaryConsultation v = consultations.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy tư vấn"));
        TransportationTrip t = v.getTrip() != null ? v.getTrip() : v.getIncident() != null ? v.getIncident().getTrip() : null;
        if (t == null || !tripAccess.canView(actor, t)) {
            throw new ForbiddenException("Bạn không có quyền xem tư vấn này");
        }
        return v;
    }

    // ================================================================== horse history
    @Transactional(readOnly = true)
    public HorseHealthHistory horseHistory(UserPrincipal actor, Long horseId) {
        if (actor.isDriverEscort()) {
            throw new ForbiddenException("Xem lịch sử theo chuyến được giao");
        }
        Horse h = horseService.loadVisible(actor, horseId);
        return new HorseHealthHistory(h.getId(), h.getRegisteredName(),
                preChecks.findByHorseIdOrderByCheckedAtDesc(horseId).stream().map(HealthMapper::toPreCheck).toList(),
                observations.findByHorseIdOrderByObservedAtDesc(horseId).stream()
                        .map(o -> HealthMapper.toObservation(o, attachmentService.refs(AttachmentRefType.HEALTH_OBSERVATION, o.getId())))
                        .toList(),
                arrivalChecks.findByHorseIdOrderByCheckedAtDesc(horseId).stream().map(HealthMapper::toArrival).toList(),
                consultations.findByHorseIdOrderByConsultedAtDesc(horseId).stream().map(HealthMapper::toVet).toList(),
                DISCLAIMER);
    }

    private Horse horseOfTrip(TransportationTrip t, Long horseId) {
        Horse horse = horses.findById(horseId).orElseThrow(() -> new NotFoundException("Không tìm thấy ngựa"));
        Guard.input(requestHorses.existsByRequestIdAndHorseId(t.getRequest().getId(), horseId), "Ngựa không thuộc chuyến này");
        return horse;
    }
}
