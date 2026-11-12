package com.example.racetransport.trip.service;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.fleet.entity.VehicleStatus;
import com.example.racetransport.handover.entity.Handover;
import com.example.racetransport.handover.repository.HandoverRepository;
import com.example.racetransport.health.entity.HealthObservation;
import com.example.racetransport.health.entity.ObservationSeverity;
import com.example.racetransport.health.entity.PreTransportHealthCheck;
import com.example.racetransport.health.repository.ArrivalConditionCheckRepository;
import com.example.racetransport.health.repository.HealthObservationRepository;
import com.example.racetransport.health.repository.PreTransportHealthCheckRepository;
import com.example.racetransport.horse.entity.HorseStatus;
import com.example.racetransport.incident.entity.Incident;
import com.example.racetransport.incident.entity.IncidentAction;
import com.example.racetransport.incident.entity.IncidentSeverity;
import com.example.racetransport.incident.repository.IncidentActionRepository;
import com.example.racetransport.incident.repository.IncidentRepository;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.route.entity.RouteStop;
import com.example.racetransport.route.entity.StopType;
import com.example.racetransport.route.repository.RouteStopRepository;
import com.example.racetransport.route.service.RouteService;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.tracking.entity.TrackingEvent;
import com.example.racetransport.tracking.entity.TrackingEventType;
import com.example.racetransport.tracking.entity.TripStatusHistory;
import com.example.racetransport.tracking.repository.TrackingEventRepository;
import com.example.racetransport.tracking.repository.TripStatusHistoryRepository;
import com.example.racetransport.tracking.service.TripStateMachine;
import com.example.racetransport.tracking.service.TripStatusService;
import com.example.racetransport.transportationrequest.entity.RequestHorse;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.trip.dto.TripDtos.CheckpointRequest;
import com.example.racetransport.trip.dto.TripDtos.StatusUpdateRequest;
import com.example.racetransport.trip.dto.TripDtos.StopProgress;
import com.example.racetransport.trip.dto.TripDtos.TimelineItem;
import com.example.racetransport.trip.dto.TripDtos.TrackingEventResponse;
import com.example.racetransport.trip.dto.TripDtos.TrackingResponse;
import com.example.racetransport.trip.dto.TripDtos.TripDetail;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStaffAssignment;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.entity.StaffRole;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trip Execution (Driver / Escort): khởi hành, cập nhật trạng thái/điểm dừng, trễ;
 * và các màn hình theo dõi (tracking, timeline) cho mọi role được xem chuyến.
 */
@Service
public class TripExecutionService {

    /** Trạng thái Driver được tự chọn khi cập nhật thủ công. */
    private static final Set<TripStatus> MANUAL = EnumSet.of(TripStatus.IN_TRANSIT, TripStatus.AT_CHECKPOINT,
            TripStatus.AT_BORDER, TripStatus.CUSTOMS_CLEARANCE, TripStatus.QUARANTINE, TripStatus.DELAYED,
            TripStatus.ARRIVED);

    private final TripAccess tripAccess;
    private final TripService tripService;
    private final TripStatusService statusService;
    private final TripStaffAssignmentRepository assignments;
    private final RequestHorseRepository requestHorses;
    private final PreTransportHealthCheckRepository preChecks;
    private final ArrivalConditionCheckRepository arrivalChecks;
    private final HealthObservationRepository observations;
    private final IncidentRepository incidents;
    private final IncidentActionRepository incidentActions;
    private final HandoverRepository handovers;
    private final RouteStopRepository routeStops;
    private final TrackingEventRepository trackingEvents;
    private final TripStatusHistoryRepository statusHistory;
    private final NotificationService notifications;
    private final AppUserRepository users;
    private final Clock clock;

    public TripExecutionService(TripAccess tripAccess, TripService tripService, TripStatusService statusService,
                                TripStaffAssignmentRepository assignments, RequestHorseRepository requestHorses,
                                PreTransportHealthCheckRepository preChecks,
                                ArrivalConditionCheckRepository arrivalChecks, HealthObservationRepository observations,
                                IncidentRepository incidents, IncidentActionRepository incidentActions,
                                HandoverRepository handovers, RouteStopRepository routeStops,
                                TrackingEventRepository trackingEvents, TripStatusHistoryRepository statusHistory,
                                NotificationService notifications, AppUserRepository users, Clock clock) {
        this.tripAccess = tripAccess;
        this.tripService = tripService;
        this.statusService = statusService;
        this.assignments = assignments;
        this.requestHorses = requestHorses;
        this.preChecks = preChecks;
        this.arrivalChecks = arrivalChecks;
        this.observations = observations;
        this.incidents = incidents;
        this.incidentActions = incidentActions;
        this.handovers = handovers;
        this.routeStops = routeStops;
        this.trackingEvents = trackingEvents;
        this.statusHistory = statusHistory;
        this.notifications = notifications;
        this.users = users;
        this.clock = clock;
    }

    // ================================================================== start
    /**
     * Bắt đầu chuyến: chỉ Driver/Escort được phân công; cần xe, ít nhất một tài xế,
     * mọi ngựa đã kiểm tra trước vận chuyển và được quan sát là phù hợp; không có sự cố chặn.
     */
    @Transactional
    public TripDetail start(UserPrincipal actor, Long tripId) {
        TransportationTrip t = tripAccess.loadAssigned(actor, tripId);
        Guard.state(t.getStatus() == TripStatus.READY_FOR_DEPARTURE,
                "Chuyến chưa sẵn sàng khởi hành (trạng thái: " + t.getStatus().getLabel() + ")");
        Guard.state(!Boolean.TRUE.equals(t.getOperationBlocked()), "Chuyến đang bị tạm dừng do sự cố chờ quyết định");
        Guard.state(t.getVehicle() != null, "Chuyến chưa được phân công phương tiện");
        Guard.state(t.getVehicle().getStatus() != VehicleStatus.MAINTENANCE
                && t.getVehicle().getStatus() != VehicleStatus.UNAVAILABLE, "Phương tiện hiện không khả dụng");
        List<TripStaffAssignment> staff = assignments.findByTripIdFetch(t.getId());
        Guard.state(staff.stream().anyMatch(a -> a.getAssignmentRole() == StaffRole.DRIVER), "Chuyến chưa có tài xế");
        Guard.state(t.getRequest().getStatus() == RequestStatus.TRIP_PLANNED, "Yêu cầu vận chuyển không ở trạng thái đã lập chuyến");
        List<RequestHorse> horses = requestHorses.findByRequestIdFetch(t.getRequest().getId());
        Map<Long, PreTransportHealthCheck> checks = preChecks.findByTripIdOrderByCheckedAtAsc(t.getId()).stream()
                .collect(Collectors.toMap(c -> c.getHorse().getId(), c -> c, (a, b) -> b));
        for (RequestHorse rh : horses) {
            PreTransportHealthCheck c = checks.get(rh.getHorse().getId());
            Guard.state(c != null, "Chưa kiểm tra trước vận chuyển cho " + rh.getHorse().getRegisteredName());
            Guard.state(Boolean.TRUE.equals(c.getFitForTravelObserved()),
                    rh.getHorse().getRegisteredName() + " chưa được xác nhận phù hợp để vận chuyển");
        }
        AppUser driver = users.getReferenceById(actor.getId());
        LocalDateTime now = LocalDateTime.now(clock);
        statusService.changeStatus(t, TripStatus.IN_TRANSIT, driver, "Khởi hành");
        t.setActualDeparture(now);
        RouteStop first = routeStops.findByRouteIdOrderByStopOrderAsc(t.getRoute().getId()).stream().findFirst().orElse(null);
        statusService.recordEvent(t, TrackingEventType.DEPARTED, first, first == null ? t.getCurrentLocation() : first.getName(),
                first == null ? null : first.getLatitude(), first == null ? null : first.getLongitude(), null,
                "Khởi hành", now, driver);
        t.getVehicle().setStatus(VehicleStatus.IN_USE);
        horses.forEach(rh -> rh.getHorse().setStatus(HorseStatus.IN_TRANSIT));
        notifications.notify(t.getRequest().getCustomer(), NotificationType.INFO, "Chuyến " + t.getTripNumber() + " đã khởi hành",
                "Bạn có thể theo dõi hành trình trong mục Chuyến đi.", NotificationService.REF_TRIP, t.getId());
        notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.INFO, "Chuyến " + t.getTripNumber() + " đã khởi hành",
                t.getRoute().getName(), NotificationService.REF_TRIP, t.getId());
        return tripService.toDetail(actor, t);
    }

    /** Gọi sau mỗi lần ghi kiểm tra trước vận chuyển: SCHEDULED → PREPARING → READY_FOR_DEPARTURE. */
    @Transactional
    public void refreshReadiness(TransportationTrip t, AppUser actor) {
        if (t.getStatus() == TripStatus.SCHEDULED) {
            statusService.changeStatus(t, TripStatus.PREPARING, actor, "Bắt đầu chuẩn bị (kiểm tra trước vận chuyển)");
        }
        List<RequestHorse> horses = requestHorses.findByRequestIdFetch(t.getRequest().getId());
        Map<Long, PreTransportHealthCheck> checks = preChecks.findByTripIdOrderByCheckedAtAsc(t.getId()).stream()
                .collect(Collectors.toMap(c -> c.getHorse().getId(), c -> c, (a, b) -> b));
        boolean allFit = horses.stream().allMatch(rh -> checks.containsKey(rh.getHorse().getId())
                && Boolean.TRUE.equals(checks.get(rh.getHorse().getId()).getFitForTravelObserved()));
        if (t.getStatus() == TripStatus.PREPARING && allFit && t.getVehicle() != null) {
            statusService.changeStatus(t, TripStatus.READY_FOR_DEPARTURE, actor, "Đã kiểm tra đủ ngựa trước vận chuyển");
        } else if (t.getStatus() == TripStatus.READY_FOR_DEPARTURE && !allFit) {
            statusService.changeStatus(t, TripStatus.PREPARING, actor, "Có ngựa chưa phù hợp vận chuyển");
        }
    }

    // ================================================================== status & checkpoints
    @Transactional
    public TripDetail updateStatus(UserPrincipal actor, Long tripId, StatusUpdateRequest req) {
        TransportationTrip t = actor.isManager() ? tripAccess.load(tripId) : tripAccess.loadAssigned(actor, tripId);
        TripStatus to = req.status();
        Guard.input(MANUAL.contains(to), "Trạng thái \"" + to.getLabel() + "\" được hệ thống đặt qua quy trình riêng");
        Guard.state(TripStateMachine.EN_ROUTE.contains(t.getStatus()), "Chuyến không ở trạng thái đang di chuyển");
        if (Boolean.TRUE.equals(t.getOperationBlocked())) {
            Guard.state(to == TripStatus.DELAYED,
                    "Chuyến đang bị tạm dừng do sự cố: chờ Logistics Manager quyết định Tiếp tục / Đổi tuyến");
        }
        if (to == TripStatus.DELAYED) {
            Guard.input(req.note() != null && !req.note().isBlank(), "Cần ghi lý do trễ");
        }
        AppUser user = users.getReferenceById(actor.getId());
        statusService.changeStatus(t, to, user, req.note());
        LocalDateTime now = LocalDateTime.now(clock);
        TrackingEventType type = switch (to) {
            case DELAYED -> TrackingEventType.DELAY;
            case ARRIVED -> TrackingEventType.ARRIVED;
            default -> TrackingEventType.STATUS_CHANGE;
        };
        statusService.recordEvent(t, type, null, req.location(), req.latitude(), req.longitude(),
                to == TripStatus.DELAYED ? req.delayMinutes() : null, req.note(), now, user);
        if (to == TripStatus.ARRIVED) {
            t.setActualArrival(now);
            List<RouteStop> stops = routeStops.findByRouteIdOrderByStopOrderAsc(t.getRoute().getId());
            if (!stops.isEmpty()) {
                t.setCurrentStop(stops.get(stops.size() - 1));
            }
            notifications.notify(t.getRequest().getCustomer(), NotificationType.ACTION_REQUIRED,
                    "Chuyến " + t.getTripNumber() + " đã đến nơi",
                    "Đội vận hành sẽ lập biên bản bàn giao; vui lòng xác nhận khi nhận ngựa.",
                    NotificationService.REF_TRIP, t.getId());
        }
        if (to == TripStatus.DELAYED) {
            notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.WARNING,
                    "Chuyến " + t.getTripNumber() + " bị trễ", req.note(), NotificationService.REF_TRIP, t.getId());
            notifications.notify(t.getRequest().getCustomer(), NotificationType.WARNING,
                    "Chuyến " + t.getTripNumber() + " bị trễ", req.note(), NotificationService.REF_TRIP, t.getId());
        }
        return tripService.toDetail(actor, t);
    }

    /** Ghi checkpoint: đến/rời điểm dừng, cập nhật vị trí, báo trễ, ghi chú. */
    @Transactional
    public TrackingEventResponse checkpoint(UserPrincipal actor, Long tripId, CheckpointRequest req) {
        TransportationTrip t = tripAccess.loadAssigned(actor, tripId);
        Guard.state(TripStateMachine.EN_ROUTE.contains(t.getStatus()), "Chỉ ghi checkpoint khi chuyến đang di chuyển");
        AppUser user = users.getReferenceById(actor.getId());
        RouteStop stop = null;
        if (req.routeStopId() != null) {
            stop = routeStops.findById(req.routeStopId())
                    .filter(s -> s.getRoute().getId().equals(t.getRoute().getId()))
                    .orElseThrow(() -> new NotFoundException("Điểm dừng không thuộc tuyến của chuyến"));
        }
        boolean blocked = Boolean.TRUE.equals(t.getOperationBlocked());
        TripStatus target = req.status();
        switch (req.eventType()) {
            case CHECKPOINT_ARRIVED -> {
                Guard.input(stop != null, "Cần chọn điểm dừng");
                if (target == null) {
                    target = statusForStop(stop.getStopType());
                }
            }
            case CHECKPOINT_DEPARTED -> {
                Guard.input(stop != null, "Cần chọn điểm dừng");
                Guard.state(!blocked, "Chuyến đang bị tạm dừng do sự cố, chưa thể rời điểm dừng");
                if (target == null) {
                    target = TripStatus.IN_TRANSIT;
                }
            }
            case DELAY -> {
                Guard.input(req.delayMinutes() != null && req.delayMinutes() > 0, "Cần nhập số phút trễ");
                Guard.input(req.note() != null && !req.note().isBlank(), "Cần ghi lý do trễ");
                target = TripStatus.DELAYED;
            }
            case LOCATION_UPDATE, NOTE -> { }
            default -> Guard.input(false, "Loại sự kiện không hợp lệ cho checkpoint");
        }
        if (target != null && target != t.getStatus()) {
            if (blocked) {
                Guard.state(target == TripStatus.DELAYED || target == TripStatus.AT_CHECKPOINT
                                || target == TripStatus.AT_BORDER || target == TripStatus.CUSTOMS_CLEARANCE
                                || target == TripStatus.QUARANTINE,
                        "Chuyến đang bị tạm dừng do sự cố chờ quyết định");
            }
            Guard.input(target != TripStatus.ARRIVED, "Dùng cập nhật trạng thái \"Đã đến nơi\" khi tới điểm cuối");
            statusService.changeStatus(t, target, user, req.note() == null ? req.eventType().getLabel() : req.note());
        }
        String location = req.location() != null && !req.location().isBlank() ? req.location()
                : stop != null ? stop.getName() : null;
        TrackingEvent e = statusService.recordEvent(t, req.eventType(), stop, location,
                req.latitude() != null ? req.latitude() : stop == null ? null : stop.getLatitude(),
                req.longitude() != null ? req.longitude() : stop == null ? null : stop.getLongitude(),
                req.delayMinutes(), req.note(), req.eventTime(), user);
        if (req.eventType() == TrackingEventType.DELAY) {
            notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.WARNING,
                    "Chuyến " + t.getTripNumber() + " trễ " + req.delayMinutes() + " phút", req.note(),
                    NotificationService.REF_TRIP, t.getId());
            notifications.notify(t.getRequest().getCustomer(), NotificationType.WARNING,
                    "Chuyến " + t.getTripNumber() + " bị trễ khoảng " + req.delayMinutes() + " phút", req.note(),
                    NotificationService.REF_TRIP, t.getId());
        } else if (stop != null && (stop.getStopType() == StopType.BORDER || stop.getStopType() == StopType.CUSTOMS)
                && req.eventType() == TrackingEventType.CHECKPOINT_ARRIVED) {
            notifications.notify(t.getRequest().getCustomer(), NotificationType.INFO,
                    "Chuyến " + t.getTripNumber() + " đến " + stop.getName(), "Đang làm thủ tục cửa khẩu/hải quan.",
                    NotificationService.REF_TRIP, t.getId());
        }
        return toEvent(e);
    }

    private static TripStatus statusForStop(StopType type) {
        return switch (type) {
            case BORDER -> TripStatus.AT_BORDER;
            case CUSTOMS -> TripStatus.CUSTOMS_CLEARANCE;
            case QUARANTINE -> TripStatus.QUARANTINE;
            default -> TripStatus.AT_CHECKPOINT;
        };
    }

    // ================================================================== views
    @Transactional(readOnly = true)
    public TrackingResponse tracking(UserPrincipal actor, Long tripId) {
        TransportationTrip t = tripAccess.loadVisible(actor, tripId);
        List<TrackingEvent> events = trackingEvents.findByTripIdOrderByEventTimeAscIdAsc(t.getId());
        List<RouteStop> stops = routeStops.findByRouteIdOrderByStopOrderAsc(t.getRoute().getId());
        Map<Long, LocalDateTime> arrived = new HashMap<>();
        Map<Long, LocalDateTime> departed = new HashMap<>();
        for (TrackingEvent e : events) {
            if (e.getRouteStop() == null) {
                continue;
            }
            Long sid = e.getRouteStop().getId();
            if (e.getEventType() == TrackingEventType.CHECKPOINT_ARRIVED) {
                arrived.putIfAbsent(sid, e.getEventTime());
            } else if (e.getEventType() == TrackingEventType.CHECKPOINT_DEPARTED || e.getEventType() == TrackingEventType.DEPARTED) {
                departed.put(sid, e.getEventTime());
                arrived.putIfAbsent(sid, e.getEventTime());
            }
        }
        Long currentStopId = t.getCurrentStop() == null ? null : t.getCurrentStop().getId();
        boolean finished = EnumSet.of(TripStatus.ARRIVED, TripStatus.HANDOVER_PENDING, TripStatus.DELIVERED).contains(t.getStatus());
        int currentIndex = -1;
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).getId().equals(currentStopId)) {
                currentIndex = i;
            }
        }
        List<StopProgress> progress = new ArrayList<>();
        for (int i = 0; i < stops.size(); i++) {
            RouteStop s = stops.get(i);
            LocalDateTime planned = s.getPlannedArrivalOffsetMin() == null ? null
                    : t.getPlannedDeparture().plusMinutes(s.getPlannedArrivalOffsetMin());
            String state;
            if (finished || i < currentIndex || departed.containsKey(s.getId()) && i != currentIndex) {
                state = "DONE";
            } else if (i == currentIndex && t.getStatus() != TripStatus.SCHEDULED) {
                state = "CURRENT";
            } else {
                state = "UPCOMING";
            }
            progress.add(new StopProgress(RouteService.toStop(s), planned, arrived.get(s.getId()),
                    departed.get(s.getId()), state));
        }
        int percent;
        if (finished) {
            percent = 100;
        } else if (TripStateMachine.PRE_DEPARTURE.contains(t.getStatus()) || stops.size() < 2) {
            percent = 0;
        } else {
            percent = Math.max(5, Math.min(95, (int) Math.round(100.0 * Math.max(currentIndex, 0) / (stops.size() - 1))));
        }
        long delay = trackingEvents.sumDelay(t.getId(), TrackingEventType.DELAY);
        LocalDateTime eta = t.getActualArrival() != null ? t.getActualArrival() : t.getPlannedArrival().plusMinutes(delay);
        LocalDateTime last = events.isEmpty() ? null : events.get(events.size() - 1).getEventTime();
        List<TrackingEventResponse> eventDtos = events.stream()
                .sorted(Comparator.comparing(TrackingEvent::getEventTime).reversed())
                .map(TripExecutionService::toEvent).toList();
        return new TrackingResponse(t.getId(), t.getTripNumber(), t.getStatus(), t.getCurrentLocation(), currentStopId,
                percent, t.getPlannedArrival(), eta, delay, last, Boolean.TRUE.equals(t.getOperationBlocked()),
                progress, eventDtos);
    }

    /** Dòng thời gian tổng hợp: trạng thái, checkpoint, quan sát sức khỏe, sự cố, hành động xử lý, bàn giao. */
    @Transactional(readOnly = true)
    public List<TimelineItem> timeline(UserPrincipal actor, Long tripId) {
        TransportationTrip t = tripAccess.loadVisible(actor, tripId);
        List<TimelineItem> items = new ArrayList<>();
        for (TripStatusHistory h : statusHistory.findByTripIdOrderByChangedAtAscIdAsc(t.getId())) {
            items.add(new TimelineItem(h.getChangedAt(), "STATUS",
                    h.getPreviousStatus() == null ? "Lập chuyến" : "Trạng thái: " + h.getNewStatus().getLabel(),
                    h.getNote(), h.getNewStatus().name(), toneOf(h.getNewStatus()), h.getChangedBy().getFullName(), h.getId()));
        }
        for (TrackingEvent e : trackingEvents.findByTripIdOrderByEventTimeAscIdAsc(t.getId())) {
            if (e.getEventType() == TrackingEventType.STATUS_CHANGE) {
                continue; // đã thể hiện qua lịch sử trạng thái
            }
            String title = e.getEventType().getLabel() + (e.getRouteStop() != null ? ": " + e.getRouteStop().getName()
                    : e.getLocation() != null ? ": " + e.getLocation() : "");
            String desc = (e.getDelayMinutes() != null ? "Trễ " + e.getDelayMinutes() + " phút. " : "")
                    + (e.getNote() == null ? "" : e.getNote());
            items.add(new TimelineItem(e.getEventTime(), "TRACKING", title, desc.isBlank() ? null : desc,
                    e.getTripStatus().name(), e.getEventType() == TrackingEventType.DELAY ? "warning" : "info",
                    e.getRecordedBy().getFullName(), e.getId()));
        }
        for (HealthObservation o : observations.findByTripIdOrderByObservedAtDesc(t.getId())) {
            items.add(new TimelineItem(o.getObservedAt(), "OBSERVATION",
                    "Quan sát sức khỏe – " + o.getHorse().getRegisteredName(),
                    "Ăn: " + o.getAppetite().getLabel() + ", uống: " + o.getWaterIntake().getLabel() + ", hành vi: "
                            + o.getBehavior().getLabel() + (o.getNotes() == null ? "" : ". " + o.getNotes()),
                    o.getSeverity().name(), o.getSeverity() == ObservationSeverity.URGENT ? "error"
                    : o.getSeverity() == ObservationSeverity.ATTENTION ? "warning" : "success",
                    o.getAuthor().getFullName(), o.getId()));
        }
        for (Incident i : incidents.findByTripIdOrderByReportedAtDesc(t.getId())) {
            items.add(new TimelineItem(i.getReportedAt(), "INCIDENT", "Sự cố " + i.getIncidentNumber() + ": " + i.getTitle(),
                    i.getSeverity().getLabel() + " – " + i.getIncidentType().getLabel(), i.getStatus().name(),
                    i.getSeverity() == IncidentSeverity.CRITICAL || i.getSeverity() == IncidentSeverity.HIGH ? "error" : "warning",
                    i.getReportedBy().getFullName(), i.getId()));
            for (IncidentAction a : incidentActions.findByIncidentIdOrderByPerformedAtAscIdAsc(i.getId())) {
                items.add(new TimelineItem(a.getPerformedAt(), "INCIDENT_ACTION",
                        a.getActionType().getLabel() + (a.getDecision() != null ? ": " + a.getDecision().getLabel() : ""),
                        a.getNote(), i.getIncidentNumber(), "primary", a.getPerformedBy().getFullName(), i.getId()));
            }
        }
        for (PreTransportHealthCheck c : preChecks.findByTripIdOrderByCheckedAtAsc(t.getId())) {
            items.add(new TimelineItem(c.getCheckedAt(), "PRE_CHECK", "Kiểm tra trước vận chuyển – " + c.getHorse().getRegisteredName(),
                    "Tình trạng: " + c.getGeneralCondition().getLabel()
                            + (Boolean.TRUE.equals(c.getFitForTravelObserved()) ? ", phù hợp vận chuyển" : ", CHƯA phù hợp"),
                    null, Boolean.TRUE.equals(c.getFitForTravelObserved()) ? "success" : "warning",
                    c.getCheckedBy().getFullName(), c.getId()));
        }
        arrivalChecks.findByTripIdOrderByCheckedAtAsc(t.getId()).forEach(c -> items.add(new TimelineItem(c.getCheckedAt(),
                "ARRIVAL_CHECK", "Kiểm tra khi đến – " + c.getHorse().getRegisteredName(),
                "Tình trạng: " + c.getHorseCondition().getLabel(), null, "info", c.getCheckedBy().getFullName(), c.getId())));
        for (Handover h : handovers.findByTripIdOrderByIdAsc(t.getId())) {
            items.add(new TimelineItem(h.getHandoverTime(), "HANDOVER", "Bàn giao " + h.getHorse().getRegisteredName()
                    + " cho " + h.getReceiverName(), h.getStatus().getLabel(), h.getStatus().name(), "primary",
                    h.getCreatedBy().getFullName(), h.getId()));
            if (h.getConfirmedAt() != null) {
                items.add(new TimelineItem(h.getConfirmedAt(), "HANDOVER", "Khách hàng xác nhận bàn giao "
                        + h.getHorse().getRegisteredName(), h.getCustomerComment(), h.getStatus().name(), "success",
                        h.getConfirmedBy() == null ? null : h.getConfirmedBy().getFullName(), h.getId()));
            }
        }
        items.sort(Comparator.comparing(TimelineItem::time, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return items;
    }

    private static String toneOf(TripStatus s) {
        return switch (s) {
            case DELAYED, CANCELLED -> "error";
            case AT_BORDER, CUSTOMS_CLEARANCE, QUARANTINE, HANDOVER_PENDING -> "warning";
            case DELIVERED, ARRIVED -> "success";
            default -> "info";
        };
    }

    static TrackingEventResponse toEvent(TrackingEvent e) {
        return new TrackingEventResponse(e.getId(), e.getEventType(), e.getEventTime(), e.getLocation(),
                e.getLatitude(), e.getLongitude(), e.getTripStatus(), e.getDelayMinutes(), e.getNote(),
                e.getRecordedBy().getFullName(), e.getRouteStop() == null ? null : e.getRouteStop().getId(),
                e.getRouteStop() == null ? null : e.getRouteStop().getName());
    }

    /** Dùng cho driver kiểm tra quyền thao tác. */
    void requireAssigned(UserPrincipal actor, TransportationTrip t) {
        if (!actor.isDriverEscort() || !tripAccess.isAssigned(actor, t)) {
            throw new ForbiddenException("Bạn không được phân công cho chuyến này");
        }
    }
}
