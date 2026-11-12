package com.example.racetransport.incident.service;

import static com.example.racetransport.incident.entity.IncidentStatus.*;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.common.util.Money;
import com.example.racetransport.document.entity.AttachmentRefType;
import com.example.racetransport.document.service.AttachmentService;
import com.example.racetransport.health.entity.VeterinaryConsultation;
import com.example.racetransport.health.mapper.HealthMapper;
import com.example.racetransport.health.repository.VeterinaryConsultationRepository;
import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.horse.repository.HorseRepository;
import com.example.racetransport.incident.dto.IncidentDtos.IncidentActionRequest;
import com.example.racetransport.incident.dto.IncidentDtos.IncidentActionResponse;
import com.example.racetransport.incident.dto.IncidentDtos.IncidentDetail;
import com.example.racetransport.incident.dto.IncidentDtos.IncidentSummary;
import com.example.racetransport.incident.dto.IncidentDtos.ReportIncidentRequest;
import com.example.racetransport.incident.dto.IncidentDtos.RouteOption;
import com.example.racetransport.incident.entity.Incident;
import com.example.racetransport.incident.entity.IncidentAction;
import com.example.racetransport.incident.entity.IncidentActionType;
import com.example.racetransport.incident.entity.IncidentSeverity;
import com.example.racetransport.incident.entity.IncidentStatus;
import com.example.racetransport.incident.entity.IncidentType;
import com.example.racetransport.incident.entity.OperationalDecision;
import com.example.racetransport.incident.repository.IncidentActionRepository;
import com.example.racetransport.incident.repository.IncidentRepository;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.route.entity.Route;
import com.example.racetransport.route.entity.RouteStatus;
import com.example.racetransport.route.repository.RouteRepository;
import com.example.racetransport.route.repository.RouteStopRepository;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.tracking.entity.TrackingEventType;
import com.example.racetransport.tracking.service.TripStateMachine;
import com.example.racetransport.tracking.service.TripStatusService;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStaffAssignment;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.trip.service.TripAccess;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Incident / Incident Action / Emergency.
 * - Sự cố mức HIGH/CRITICAL tự động tạm dừng chuyến (DELAYED + operationBlocked) cho tới khi
 *   Logistics Manager ghi quyết định Continue / Monitor / Stop / Reroute.
 * - Incident KHÔNG tự động trở thành Claim.
 */
@Service
public class IncidentService {

    private static final Set<IncidentSeverity> BLOCKING = EnumSet.of(IncidentSeverity.HIGH, IncidentSeverity.CRITICAL);
    private static final Set<IncidentType> COMPLIANCE_RELATED = EnumSet.of(IncidentType.DOCUMENT_ISSUE,
            IncidentType.HORSE_HEALTH, IncidentType.HORSE_INJURY, IncidentType.BORDER_DELAY);
    private static final Set<IncidentStatus> OPEN = EnumSet.of(IncidentStatus.OPEN, UNDER_REVIEW, ACTION_REQUIRED, MONITORING);

    private final IncidentRepository incidents;
    private final IncidentActionRepository actions;
    private final VeterinaryConsultationRepository consultations;
    private final TripStaffAssignmentRepository assignments;
    private final RequestHorseRepository requestHorses;
    private final HorseRepository horses;
    private final RouteRepository routes;
    private final RouteStopRepository routeStops;
    private final TripAccess tripAccess;
    private final TripStatusService statusService;
    private final AttachmentService attachmentService;
    private final NotificationService notifications;
    private final AppUserRepository users;
    private final CodeGenerator codes;
    private final Clock clock;

    public IncidentService(IncidentRepository incidents, IncidentActionRepository actions,
                           VeterinaryConsultationRepository consultations, TripStaffAssignmentRepository assignments,
                           RequestHorseRepository requestHorses, HorseRepository horses, RouteRepository routes,
                           RouteStopRepository routeStops, TripAccess tripAccess, TripStatusService statusService,
                           AttachmentService attachmentService, NotificationService notifications,
                           AppUserRepository users, CodeGenerator codes, Clock clock) {
        this.incidents = incidents;
        this.actions = actions;
        this.consultations = consultations;
        this.assignments = assignments;
        this.requestHorses = requestHorses;
        this.horses = horses;
        this.routes = routes;
        this.routeStops = routeStops;
        this.tripAccess = tripAccess;
        this.statusService = statusService;
        this.attachmentService = attachmentService;
        this.notifications = notifications;
        this.users = users;
        this.codes = codes;
        this.clock = clock;
    }

    // ================================================================== report
    @Transactional
    public IncidentDetail report(UserPrincipal actor, ReportIncidentRequest req) {
        TransportationTrip t = actor.isManager() ? tripAccess.load(req.tripId()) : tripAccess.loadAssigned(actor, req.tripId());
        Guard.state(TripStateMachine.ACTIVE.contains(t.getStatus()), "Chuyến đã kết thúc, không thể báo sự cố");
        Horse horse = null;
        if (req.horseId() != null) {
            horse = horses.findById(req.horseId()).orElseThrow(() -> new NotFoundException("Không tìm thấy ngựa"));
            Guard.input(requestHorses.existsByRequestIdAndHorseId(t.getRequest().getId(), horse.getId()),
                    "Ngựa không thuộc chuyến này");
        }
        if (req.incidentType() == IncidentType.HORSE_HEALTH || req.incidentType() == IncidentType.HORSE_INJURY) {
            Guard.input(horse != null, "Sự cố liên quan ngựa cần chọn ngựa");
        }
        return toDetail(actor, createIncident(t, horse, req.incidentType(), req.severity(), req.title(),
                req.description(), req.location(), req.occurredAt(), users.getReferenceById(actor.getId())));
    }

    /** Dùng chung cho báo cáo trực tiếp và báo từ quan sát sức khỏe. */
    @Transactional
    public Incident createIncident(TransportationTrip t, Horse horse, IncidentType type, IncidentSeverity severity,
                                   String title, String description, String location, LocalDateTime occurredAt,
                                   AppUser reporter) {
        LocalDateTime now = LocalDateTime.now(clock);
        Incident i = new Incident();
        i.setIncidentNumber(codes.next(CodeGenerator.INCIDENT));
        i.setTrip(t);
        i.setHorse(horse);
        i.setReportedBy(reporter);
        i.setIncidentType(type);
        i.setSeverity(severity);
        i.setTitle(title.trim());
        i.setDescription(description.trim());
        i.setLocation(location == null || location.isBlank() ? t.getCurrentLocation() : location);
        i.setStatus(IncidentStatus.OPEN);
        i.setReportedAt(occurredAt == null || occurredAt.isAfter(now) ? now : occurredAt);
        incidents.save(i);

        boolean blocking = BLOCKING.contains(severity);
        if (blocking) {
            // Không tự động cho phép tiếp tục: tạm dừng chuyến chờ quyết định
            t.setOperationBlocked(true);
            if (TripStateMachine.EN_ROUTE.contains(t.getStatus()) && t.getStatus() != TripStatus.DELAYED) {
                statusService.changeStatus(t, TripStatus.DELAYED, reporter,
                        "Tạm dừng do sự cố " + i.getIncidentNumber() + " (" + severity.getLabel() + ")");
            }
            statusService.recordEvent(t, TrackingEventType.DELAY, null, i.getLocation(), null, null, null,
                    "Sự cố " + i.getIncidentNumber() + ": " + i.getTitle(), now, reporter);
        }
        NotificationType nt = severity == IncidentSeverity.CRITICAL ? NotificationType.CRITICAL
                : blocking ? NotificationType.ACTION_REQUIRED : NotificationType.WARNING;
        String msg = i.getTitle() + " – chuyến " + t.getTripNumber() + (horse == null ? "" : ", ngựa " + horse.getRegisteredName())
                + (blocking ? ". Chuyến đang tạm dừng chờ quyết định." : "");
        notifications.notifyRole(Role.LOGISTICS_MANAGER, nt, "Sự cố " + severity.getLabel() + ": " + i.getIncidentNumber(),
                msg, NotificationService.REF_INCIDENT, i.getId());
        if (COMPLIANCE_RELATED.contains(type)) {
            notifications.notifyRole(Role.TRANSPORT_SPECIALIST, NotificationType.WARNING,
                    "Sự cố liên quan hồ sơ/sức khỏe: " + i.getIncidentNumber(), msg, NotificationService.REF_INCIDENT, i.getId());
        }
        if (blocking) {
            notifications.notify(t.getRequest().getCustomer(), NotificationType.WARNING,
                    "Thông báo sự cố trên chuyến " + t.getTripNumber(),
                    "Đội vận hành đang xử lý: " + i.getTitle() + ". Chúng tôi sẽ cập nhật ngay khi có quyết định.",
                    NotificationService.REF_TRIP, t.getId());
        }
        return i;
    }

    // ================================================================== actions & decisions
    @Transactional
    public IncidentDetail addAction(UserPrincipal actor, Long id, IncidentActionRequest req) {
        Incident i = load(id);
        TransportationTrip t = i.getTrip();
        boolean assigned = actor.isDriverEscort() && tripAccess.isAssigned(actor, t);
        if (!actor.isManager() && !actor.isSpecialist() && !assigned) {
            throw new ForbiddenException("Bạn không có quyền xử lý sự cố này");
        }
        Guard.state(OPEN.contains(i.getStatus()), "Sự cố đã " + i.getStatus().getLabel().toLowerCase());
        Guard.input(req.actionType() != IncidentActionType.STATUS_CHANGE, "Dùng API cập nhật trạng thái sự cố");
        AppUser user = users.getReferenceById(actor.getId());
        IncidentAction a = new IncidentAction();
        a.setIncident(i);
        a.setActionType(req.actionType());
        a.setNote(req.note().trim());
        a.setPerformedBy(user);
        a.setPerformedAt(LocalDateTime.now(clock));
        if (req.consultationId() != null) {
            VeterinaryConsultation vc = consultations.findById(req.consultationId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy bản ghi tư vấn thú y"));
            a.setConsultation(vc);
        }
        if (req.actionType() == IncidentActionType.DECISION) {
            if (!actor.isManager()) {
                throw new ForbiddenException("Chỉ Logistics Manager quyết định Continue / Monitor / Stop / Reroute");
            }
            Guard.input(req.decision() != null, "Cần chọn quyết định vận hành");
            a.setDecision(req.decision());
            applyDecision(i, req, a, user);
        } else if (i.getStatus() == IncidentStatus.OPEN && (actor.isManager() || actor.isSpecialist())) {
            i.setStatus(UNDER_REVIEW);
        }
        actions.save(a);
        return toDetail(actor, i);
    }

    private void applyDecision(Incident i, IncidentActionRequest req, IncidentAction a, AppUser manager) {
        TransportationTrip t = i.getTrip();
        LocalDateTime now = LocalDateTime.now(clock);
        OperationalDecision d = req.decision();
        i.setDecision(d);
        i.setDecidedBy(manager);
        i.setDecidedAt(now);
        boolean enRoute = TripStateMachine.EN_ROUTE.contains(t.getStatus());
        switch (d) {
            case CONTINUE, MONITOR -> {
                i.setStatus(d == OperationalDecision.CONTINUE ? RESOLVED : MONITORING);
                if (d == OperationalDecision.CONTINUE) {
                    i.setResolvedAt(now);
                    i.setResolutionNote(req.note());
                }
                unblockIfClear(t, i, manager, "Quyết định " + d.getLabel() + " cho sự cố " + i.getIncidentNumber());
            }
            case STOP -> {
                i.setStatus(ACTION_REQUIRED);
                t.setOperationBlocked(true);
                if (enRoute && t.getStatus() != TripStatus.DELAYED) {
                    statusService.changeStatus(t, TripStatus.DELAYED, manager, "Dừng tại chỗ theo quyết định " + i.getIncidentNumber());
                }
            }
            case REROUTE -> {
                Guard.input(req.newRouteId() != null, "Cần chọn tuyến thay thế");
                Route newRoute = routes.findById(req.newRouteId())
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy tuyến thay thế"));
                Guard.state(newRoute.getStatus() == RouteStatus.ACTIVE, "Tuyến thay thế chưa ở trạng thái Đang dùng");
                Guard.input(!newRoute.getId().equals(t.getRoute().getId()), "Tuyến thay thế phải khác tuyến hiện tại");
                Guard.input(routeStops.countByRouteId(newRoute.getId()) >= 2, "Tuyến thay thế cần ít nhất 2 điểm dừng");
                a.setNewRoute(newRoute);
                if (req.extraCost() != null) {
                    a.setExtraCost(Money.of(req.extraCost()));
                    a.setCurrency(req.currency() == null ? "USD" : req.currency());
                }
                String oldName = t.getRoute().getName();
                t.setRoute(newRoute);
                t.setCurrentStop(null);
                i.setStatus(MONITORING);
                statusService.recordEvent(t, TrackingEventType.NOTE, null, t.getCurrentLocation(), null, null, null,
                        "Đổi tuyến: " + oldName + " → " + newRoute.getName() + " (" + i.getIncidentNumber() + ")", now, manager);
                unblockIfClear(t, i, manager, "Đổi tuyến theo quyết định " + i.getIncidentNumber());
            }
            default -> { }
        }
        String message = d.getLabel() + ": " + req.note();
        List<AppUser> staff = assignments.findByTripIdFetch(t.getId()).stream().map(TripStaffAssignment::getStaff).toList();
        notifications.notifyAll(staff, d == OperationalDecision.STOP ? NotificationType.CRITICAL : NotificationType.ACTION_REQUIRED,
                "Quyết định cho sự cố " + i.getIncidentNumber(), message, NotificationService.REF_INCIDENT, i.getId());
        notifications.notify(t.getRequest().getCustomer(), NotificationType.INFO,
                "Cập nhật xử lý sự cố trên chuyến " + t.getTripNumber(), d.getLabel() + ".", NotificationService.REF_TRIP, t.getId());
    }

    /** Bỏ chặn chuyến nếu không còn sự cố nghiêm trọng nào khác chưa có quyết định; DELAYED → IN_TRANSIT. */
    private void unblockIfClear(TransportationTrip t, Incident current, AppUser manager, String note) {
        if (incidents.existsOtherBlocking(t.getId(), current.getId())) {
            return;
        }
        t.setOperationBlocked(false);
        if (t.getStatus() == TripStatus.DELAYED) {
            statusService.changeStatus(t, TripStatus.IN_TRANSIT, manager, note);
        }
    }

    /** Manager cập nhật trạng thái sự cố; sự cố HIGH/CRITICAL chỉ được đóng khi đã có quyết định. */
    @Transactional
    public IncidentDetail changeStatus(UserPrincipal actor, Long id, IncidentStatus to, String note) {
        if (!actor.isManager()) {
            throw new ForbiddenException("Chỉ Logistics Manager cập nhật trạng thái sự cố");
        }
        Incident i = load(id);
        IncidentStatus from = i.getStatus();
        Map<IncidentStatus, Set<IncidentStatus>> allowed = new LinkedHashMap<>();
        allowed.put(IncidentStatus.OPEN, EnumSet.of(UNDER_REVIEW, ACTION_REQUIRED, MONITORING, RESOLVED));
        allowed.put(UNDER_REVIEW, EnumSet.of(ACTION_REQUIRED, MONITORING, RESOLVED));
        allowed.put(ACTION_REQUIRED, EnumSet.of(UNDER_REVIEW, MONITORING, RESOLVED));
        allowed.put(MONITORING, EnumSet.of(ACTION_REQUIRED, RESOLVED));
        allowed.put(RESOLVED, EnumSet.of(CLOSED, MONITORING));
        allowed.put(CLOSED, EnumSet.noneOf(IncidentStatus.class));
        Guard.state(allowed.get(from).contains(to), "Không thể chuyển sự cố từ \"" + from.getLabel() + "\" sang \"" + to.getLabel() + "\"");
        if ((to == RESOLVED || to == CLOSED) && BLOCKING.contains(i.getSeverity())) {
            Guard.state(i.getDecision() != null && i.getDecision() != OperationalDecision.STOP,
                    "Sự cố mức " + i.getSeverity().getLabel() + " cần quyết định Tiếp tục / Theo dõi / Đổi tuyến trước khi đóng");
        }
        if (to == RESOLVED) {
            Guard.input(note != null && !note.isBlank(), "Cần ghi kết quả xử lý");
            i.setResolvedAt(LocalDateTime.now(clock));
            i.setResolutionNote(note);
        }
        i.setStatus(to);
        AppUser user = users.getReferenceById(actor.getId());
        IncidentAction a = new IncidentAction();
        a.setIncident(i);
        a.setActionType(IncidentActionType.STATUS_CHANGE);
        a.setNote(from.getLabel() + " → " + to.getLabel() + (note == null || note.isBlank() ? "" : ": " + note));
        a.setPerformedBy(user);
        a.setPerformedAt(LocalDateTime.now(clock));
        actions.save(a);
        if (to == RESOLVED || to == CLOSED) {
            unblockIfClear(i.getTrip(), i, user, "Sự cố " + i.getIncidentNumber() + " đã xử lý");
        }
        return toDetail(actor, i);
    }

    // ================================================================== queries
    @Transactional(readOnly = true)
    public PageResponse<IncidentSummary> list(UserPrincipal actor, Collection<IncidentStatus> statuses,
                                              Collection<IncidentSeverity> severities, Long tripId, int page, int size) {
        var result = incidents.search(statuses == null || statuses.isEmpty() ? EnumSet.allOf(IncidentStatus.class) : statuses,
                severities == null || severities.isEmpty() ? EnumSet.allOf(IncidentSeverity.class) : severities, tripId,
                actor.isCustomer() ? actor.getId() : null, actor.isDriverEscort() ? actor.getId() : null,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "reportedAt")));
        return PageResponse.of(result, IncidentService::toSummary);
    }

    @Transactional(readOnly = true)
    public IncidentDetail get(UserPrincipal actor, Long id) {
        Incident i = load(id);
        if (!tripAccess.canView(actor, i.getTrip())) {
            throw new ForbiddenException("Bạn không có quyền xem sự cố này");
        }
        return toDetail(actor, i);
    }

    public Incident load(Long id) {
        return incidents.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy sự cố"));
    }

    static IncidentSummary toSummary(Incident i) {
        TransportationTrip t = i.getTrip();
        return new IncidentSummary(i.getId(), i.getIncidentNumber(), t.getId(), t.getTripNumber(), t.getRequest().getId(),
                t.getRequest().getRequestNumber(), t.getRequest().getCustomer().getFullName(),
                i.getHorse() == null ? null : i.getHorse().getId(),
                i.getHorse() == null ? null : i.getHorse().getRegisteredName(), i.getIncidentType(), i.getSeverity(),
                i.getTitle(), i.getStatus(), i.getDecision(), i.getLocation(), i.getReportedAt(),
                i.getReportedBy().getFullName(), Boolean.TRUE.equals(t.getOperationBlocked()));
    }

    private IncidentDetail toDetail(UserPrincipal actor, Incident i) {
        TransportationTrip t = i.getTrip();
        List<IncidentActionResponse> actionList = actions.findByIncidentIdOrderByPerformedAtAscIdAsc(i.getId()).stream()
                .map(a -> new IncidentActionResponse(a.getId(), a.getActionType(), a.getDecision(), a.getNote(),
                        a.getPerformedBy().getFullName(), a.getPerformedBy().getRole(), a.getPerformedAt(),
                        a.getConsultation() == null ? null : a.getConsultation().getId(),
                        a.getNewRoute() == null ? null : a.getNewRoute().getId(),
                        a.getNewRoute() == null ? null : a.getNewRoute().getName(), a.getExtraCost(), a.getCurrency()))
                .toList();
        List<RouteOption> options = new ArrayList<>();
        if (actor.isManager() && OPEN.contains(i.getStatus())) {
            routes.findByAlternativeOfId(t.getRoute().getId()).stream().filter(r -> r.getStatus() == RouteStatus.ACTIVE)
                    .forEach(r -> options.add(new RouteOption(r.getId(), r.getRouteCode(), r.getName())));
            routes.search(EnumSet.of(RouteStatus.ACTIVE), null, t.getRoute().getDestinationCountry(), null).stream()
                    .filter(r -> !r.getId().equals(t.getRoute().getId()))
                    .filter(r -> options.stream().noneMatch(o -> o.id().equals(r.getId())))
                    .forEach(r -> options.add(new RouteOption(r.getId(), r.getRouteCode(), r.getName())));
        }
        List<String> allowed = new ArrayList<>();
        boolean open = OPEN.contains(i.getStatus());
        if (open && (actor.isManager() || actor.isSpecialist() || (actor.isDriverEscort() && tripAccess.isAssigned(actor, t)))) {
            allowed.add("ADD_ACTION");
            allowed.add("RECORD_VET_CONSULTATION");
        }
        if (actor.isManager()) {
            if (open) {
                allowed.add("DECIDE");
            }
            if (i.getStatus() != CLOSED) {
                allowed.add("CHANGE_STATUS");
            }
        }
        if (actor.isCustomer() && (i.getStatus() == RESOLVED || i.getStatus() == CLOSED || i.getStatus() == MONITORING)) {
            allowed.add("CREATE_CLAIM");
        }
        return new IncidentDetail(i.getId(), i.getIncidentNumber(), t.getId(), t.getTripNumber(), t.getStatus(),
                t.getCurrentLocation(), t.getRoute().getName(), t.getRoute().getId(), t.getRequest().getId(),
                t.getRequest().getRequestNumber(), t.getRequest().getCustomer().getFullName(),
                i.getHorse() == null ? null : i.getHorse().getId(),
                i.getHorse() == null ? null : i.getHorse().getRegisteredName(), i.getIncidentType(), i.getSeverity(),
                i.getTitle(), i.getDescription(), i.getLocation(), i.getStatus(), i.getReportedAt(),
                i.getReportedBy().getFullName(), i.getDecision(),
                i.getDecidedBy() == null ? null : i.getDecidedBy().getFullName(), i.getDecidedAt(), i.getResolvedAt(),
                i.getResolutionNote(), Boolean.TRUE.equals(t.getOperationBlocked()), actionList,
                consultations.findByIncidentIdOrderByConsultedAtDesc(i.getId()).stream().map(HealthMapper::toVet).toList(),
                attachmentService.refs(AttachmentRefType.INCIDENT, i.getId()), options, allowed);
    }
}
