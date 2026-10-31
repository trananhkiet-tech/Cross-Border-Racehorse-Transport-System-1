package com.example.racetransport.trip.service;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.fleet.entity.Vehicle;
import com.example.racetransport.fleet.entity.VehicleStatus;
import com.example.racetransport.fleet.repository.VehicleRepository;
import com.example.racetransport.handover.entity.Handover;
import com.example.racetransport.handover.repository.HandoverRepository;
import com.example.racetransport.health.entity.ArrivalConditionCheck;
import com.example.racetransport.health.entity.PreTransportHealthCheck;
import com.example.racetransport.health.repository.ArrivalConditionCheckRepository;
import com.example.racetransport.health.repository.PreTransportHealthCheckRepository;
import com.example.racetransport.incident.repository.IncidentRepository;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.route.entity.Route;
import com.example.racetransport.route.entity.RouteStatus;
import com.example.racetransport.route.repository.RouteRepository;
import com.example.racetransport.route.repository.RouteStopRepository;
import com.example.racetransport.route.service.RouteService;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.tracking.entity.TrackingEventType;
import com.example.racetransport.tracking.repository.TrackingEventRepository;
import com.example.racetransport.tracking.service.TripStateMachine;
import com.example.racetransport.tracking.service.TripStatusService;
import com.example.racetransport.transportationrequest.entity.RequestHorse;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.transportationrequest.service.RequestAccess;
import com.example.racetransport.transportationrequest.service.RequestWorkflow;
import com.example.racetransport.trip.dto.TripDtos.CreateTripRequest;
import com.example.racetransport.trip.dto.TripDtos.IncidentRef;
import com.example.racetransport.trip.dto.TripDtos.StaffAssignmentInput;
import com.example.racetransport.trip.dto.TripDtos.StaffResponse;
import com.example.racetransport.trip.dto.TripDtos.TripDetail;
import com.example.racetransport.trip.dto.TripDtos.TripHorseResponse;
import com.example.racetransport.trip.dto.TripDtos.TripSummary;
import com.example.racetransport.trip.dto.TripDtos.UpdateTripPlanRequest;
import com.example.racetransport.trip.dto.TripDtos.VehicleRef;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStaffAssignment;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.EmployeeProfile;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.entity.StaffRole;
import com.example.racetransport.user.repository.AppUserRepository;
import com.example.racetransport.user.repository.EmployeeProfileRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trip Planning (Fleet & Route Coordinator): lập chuyến, phân công phương tiện và Driver/Escort,
 * kèm các màn hình xem chi tiết chuyến cho mọi role được phép.
 */
@Service
public class TripService {

    private final TransportationTripRepository trips;
    private final TripStaffAssignmentRepository assignments;
    private final RouteRepository routes;
    private final RouteStopRepository routeStops;
    private final VehicleRepository vehicles;
    private final AppUserRepository users;
    private final EmployeeProfileRepository employees;
    private final RequestHorseRepository requestHorses;
    private final PreTransportHealthCheckRepository preChecks;
    private final ArrivalConditionCheckRepository arrivalChecks;
    private final HandoverRepository handovers;
    private final IncidentRepository incidents;
    private final TrackingEventRepository trackingEvents;
    private final RequestAccess requestAccess;
    private final RequestWorkflow requestWorkflow;
    private final TripAccess tripAccess;
    private final TripStatusService statusService;
    private final NotificationService notifications;
    private final CodeGenerator codes;
    private final Clock clock;

    public TripService(TransportationTripRepository trips, TripStaffAssignmentRepository assignments,
                       RouteRepository routes, RouteStopRepository routeStops, VehicleRepository vehicles,
                       AppUserRepository users, EmployeeProfileRepository employees,
                       RequestHorseRepository requestHorses, PreTransportHealthCheckRepository preChecks,
                       ArrivalConditionCheckRepository arrivalChecks, HandoverRepository handovers,
                       IncidentRepository incidents, TrackingEventRepository trackingEvents,
                       RequestAccess requestAccess, RequestWorkflow requestWorkflow, TripAccess tripAccess,
                       TripStatusService statusService, NotificationService notifications, CodeGenerator codes,
                       Clock clock) {
        this.trips = trips;
        this.assignments = assignments;
        this.routes = routes;
        this.routeStops = routeStops;
        this.vehicles = vehicles;
        this.users = users;
        this.employees = employees;
        this.requestHorses = requestHorses;
        this.preChecks = preChecks;
        this.arrivalChecks = arrivalChecks;
        this.handovers = handovers;
        this.incidents = incidents;
        this.trackingEvents = trackingEvents;
        this.requestAccess = requestAccess;
        this.requestWorkflow = requestWorkflow;
        this.tripAccess = tripAccess;
        this.statusService = statusService;
        this.notifications = notifications;
        this.codes = codes;
        this.clock = clock;
    }

    // ================================================================== queries
    @Transactional(readOnly = true)
    public PageResponse<TripSummary> list(UserPrincipal actor, Collection<TripStatus> statuses, String q, int page,
                                          int size) {
        Long customerId = actor.isCustomer() ? actor.getId() : null;
        Long staffId = actor.isDriverEscort() ? actor.getId() : null;
        var result = trips.search(statuses == null || statuses.isEmpty() ? EnumSet.allOf(TripStatus.class) : statuses,
                customerId, staffId, q == null || q.isBlank() ? null : q.trim(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.ASC, "plannedDeparture")));
        List<Long> ids = result.getContent().stream().map(TransportationTrip::getId).toList();
        Map<Long, List<String>> staffNames = ids.isEmpty() ? Map.of() : assignments.findByTripIdIn(ids).stream()
                .collect(Collectors.groupingBy(a -> a.getTrip().getId(),
                        Collectors.mapping(a -> a.getStaff().getFullName(), Collectors.toList())));
        return PageResponse.of(result, t -> toSummary(t, staffNames.getOrDefault(t.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public TripDetail get(UserPrincipal actor, Long id) {
        return toDetail(actor, tripAccess.loadVisible(actor, id));
    }

    // ================================================================== planning
    @Transactional
    public TripDetail create(UserPrincipal actor, CreateTripRequest req) {
        requireCoordinator(actor);
        TransportationRequest request = requestAccess.load(req.requestId());
        Guard.state(request.getStatus() == RequestStatus.READY_FOR_PLANNING || request.getStatus() == RequestStatus.TRIP_PLANNED,
                "Yêu cầu chưa sẵn sàng lập chuyến (cần đã chấp nhận báo giá và đặt cọc)");
        Route route = routes.findById(req.routeId()).orElseThrow(() -> new NotFoundException("Không tìm thấy tuyến"));
        Guard.state(route.getStatus() == RouteStatus.ACTIVE, "Tuyến chưa ở trạng thái Đang dùng");
        Guard.input(routeStops.countByRouteId(route.getId()) >= 2, "Tuyến cần ít nhất 2 điểm dừng (nhận và đến)");
        validateWindow(req.plannedDeparture(), req.plannedArrival());

        AppUser coordinator = users.getReferenceById(actor.getId());
        TransportationTrip t = new TransportationTrip();
        t.setTripNumber(codes.next(CodeGenerator.TRIP));
        t.setRequest(request);
        t.setRoute(route);
        t.setCoordinator(coordinator);
        t.setPlannedDeparture(req.plannedDeparture());
        t.setPlannedArrival(req.plannedArrival());
        t.setStatus(TripStatus.SCHEDULED);
        t.setOperationBlocked(false);
        t.setCurrentLocation(route.getOriginLocation());
        t.setTransportInstructions(req.transportInstructions());
        t.setNotes(req.notes());
        trips.saveAndFlush(t);
        statusService.recordCreated(t, coordinator);
        if (req.vehicleId() != null) {
            doAssignVehicle(t, req.vehicleId());
        }
        if (req.staff() != null) {
            for (StaffAssignmentInput s : req.staff()) {
                doAssignStaff(t, s.staffId(), s.role(), coordinator);
            }
        }
        if (request.getStatus() == RequestStatus.READY_FOR_PLANNING) {
            requestWorkflow.transition(request, RequestStatus.TRIP_PLANNED, coordinator, "Lập chuyến " + t.getTripNumber());
        }
        notifications.notify(request.getCustomer(), NotificationType.INFO,
                "Đã lập chuyến " + t.getTripNumber() + " cho " + request.getRequestNumber(),
                "Khởi hành dự kiến " + t.getPlannedDeparture() + " theo tuyến " + route.getName() + ".",
                NotificationService.REF_TRIP, t.getId());
        notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.INFO, "Chuyến mới " + t.getTripNumber(),
                request.getRequestNumber() + " – " + route.getName(), NotificationService.REF_TRIP, t.getId());
        return toDetail(actor, t);
    }

    @Transactional
    public TripDetail updatePlan(UserPrincipal actor, Long id, UpdateTripPlanRequest req) {
        requireCoordinator(actor);
        TransportationTrip t = tripAccess.load(id);
        Guard.state(TripStateMachine.PRE_DEPARTURE.contains(t.getStatus()), "Chỉ sửa kế hoạch trước khi khởi hành");
        validateWindow(req.plannedDeparture(), req.plannedArrival());
        t.setPlannedDeparture(req.plannedDeparture());
        t.setPlannedArrival(req.plannedArrival());
        // Kiểm tra lại trùng lịch với khung giờ mới
        if (t.getVehicle() != null) {
            Guard.state(!trips.existsVehicleOverlap(t.getVehicle().getId(), t.getId(), t.getPlannedDeparture(),
                    t.getPlannedArrival()), "Phương tiện bị trùng lịch với khung giờ mới");
        }
        for (TripStaffAssignment a : assignments.findByTripIdFetch(t.getId())) {
            Guard.state(!assignments.existsStaffOverlap(a.getStaff().getId(), t.getId(), t.getPlannedDeparture(),
                    t.getPlannedArrival()), a.getStaff().getFullName() + " bị trùng lịch với khung giờ mới");
        }
        t.setTransportInstructions(req.transportInstructions());
        t.setNotes(req.notes());
        return toDetail(actor, t);
    }

    @Transactional
    public TripDetail assignVehicle(UserPrincipal actor, Long tripId, Long vehicleId) {
        requireCoordinator(actor);
        TransportationTrip t = tripAccess.load(tripId);
        doAssignVehicle(t, vehicleId);
        notifyStaff(t, NotificationType.INFO, "Phương tiện cho chuyến " + t.getTripNumber(),
                "Xe " + t.getVehicle().getVehicleCode() + " (" + t.getVehicle().getPlateNumber() + ")");
        return toDetail(actor, t);
    }

    @Transactional
    public TripDetail assignStaff(UserPrincipal actor, Long tripId, Long staffId, StaffRole role) {
        requireCoordinator(actor);
        TransportationTrip t = tripAccess.load(tripId);
        TripStaffAssignment a = doAssignStaff(t, staffId, role, users.getReferenceById(actor.getId()));
        notifications.notify(a.getStaff(), NotificationType.ACTION_REQUIRED,
                "Bạn được phân công chuyến " + t.getTripNumber(),
                "Vai trò: " + role.getLabel() + ". Khởi hành dự kiến " + t.getPlannedDeparture() + ".",
                NotificationService.REF_TRIP, t.getId());
        return toDetail(actor, t);
    }

    @Transactional
    public TripDetail unassignStaff(UserPrincipal actor, Long tripId, Long staffId) {
        requireCoordinator(actor);
        TransportationTrip t = tripAccess.load(tripId);
        Guard.state(TripStateMachine.PRE_DEPARTURE.contains(t.getStatus()), "Chỉ thay đổi nhân sự trước khi khởi hành");
        TripStaffAssignment a = assignments.findByTripIdAndStaffId(tripId, staffId)
                .orElseThrow(() -> new NotFoundException("Nhân viên chưa được phân công cho chuyến này"));
        assignments.delete(a);
        notifications.notify(a.getStaff(), NotificationType.INFO, "Hủy phân công chuyến " + t.getTripNumber(),
                "Bạn không còn được phân công cho chuyến này.", NotificationService.REF_TRIP, t.getId());
        return toDetail(actor, t);
    }

    @Transactional
    public TripDetail cancel(UserPrincipal actor, Long tripId, String note) {
        if (!actor.isCoordinator() && !actor.isManager()) {
            throw new ForbiddenException("Chỉ Coordinator hoặc Manager được hủy chuyến");
        }
        Guard.input(note != null && !note.isBlank(), "Cần ghi lý do hủy chuyến");
        TransportationTrip t = tripAccess.load(tripId);
        Guard.state(TripStateMachine.PRE_DEPARTURE.contains(t.getStatus()), "Chỉ hủy chuyến chưa khởi hành");
        AppUser user = users.getReferenceById(actor.getId());
        statusService.changeStatus(t, TripStatus.CANCELLED, user, note);
        TransportationRequest r = t.getRequest();
        boolean otherActive = trips.findByRequestIdOrderByPlannedDepartureAsc(r.getId()).stream()
                .anyMatch(x -> !x.getId().equals(t.getId()) && TripStateMachine.ACTIVE.contains(x.getStatus()));
        if (!otherActive && r.getStatus() == RequestStatus.TRIP_PLANNED) {
            requestWorkflow.transition(r, RequestStatus.READY_FOR_PLANNING, user, "Hủy chuyến " + t.getTripNumber() + ": " + note);
        }
        notifyStaff(t, NotificationType.WARNING, "Chuyến " + t.getTripNumber() + " đã bị hủy", note);
        notifications.notify(r.getCustomer(), NotificationType.WARNING, "Chuyến " + t.getTripNumber() + " đã bị hủy",
                note, NotificationService.REF_TRIP, t.getId());
        return toDetail(actor, t);
    }

    // ================================================================== rules
    private void validateWindow(LocalDateTime dep, LocalDateTime arr) {
        Guard.input(arr.isAfter(dep), "Giờ đến dự kiến phải sau giờ khởi hành");
        Guard.input(dep.isAfter(LocalDateTime.now(clock).minusHours(1)), "Giờ khởi hành phải ở tương lai");
        Guard.input(java.time.Duration.between(dep, arr).toDays() <= 30, "Một chuyến không kéo dài quá 30 ngày");
    }

    /** Không phân công xe không sẵn sàng, không đủ chỗ hoặc trùng lịch với chuyến khác. */
    void doAssignVehicle(TransportationTrip t, Long vehicleId) {
        Guard.state(TripStateMachine.PRE_DEPARTURE.contains(t.getStatus()), "Chỉ phân công xe trước khi khởi hành");
        Vehicle v = vehicles.findById(vehicleId).orElseThrow(() -> new NotFoundException("Không tìm thấy phương tiện"));
        Guard.state(!Boolean.TRUE.equals(v.getArchived()), "Phương tiện đã lưu trữ");
        Guard.state(v.getStatus() == VehicleStatus.AVAILABLE || v.getStatus() == VehicleStatus.IN_USE,
                "Phương tiện " + v.getVehicleCode() + " đang " + v.getStatus().getLabel().toLowerCase());
        long horses = requestHorses.countByRequestId(t.getRequest().getId());
        Guard.state(v.getCapacityHorses() >= horses,
                "Xe chỉ chở được " + v.getCapacityHorses() + " ngựa, yêu cầu có " + horses + " ngựa");
        Guard.state(!trips.existsVehicleOverlap(v.getId(), t.getId(), t.getPlannedDeparture(), t.getPlannedArrival()),
                "Phương tiện " + v.getVehicleCode() + " đã được giao cho chuyến khác trùng khung giờ");
        t.setVehicle(v);
    }

    /** Chỉ phân công nhân viên DRIVER_ESCORT đang hoạt động, không trùng lịch; tài xế cần GPLX còn hạn. */
    TripStaffAssignment doAssignStaff(TransportationTrip t, Long staffId, StaffRole role, AppUser assignedBy) {
        Guard.state(TripStateMachine.PRE_DEPARTURE.contains(t.getStatus()), "Chỉ phân công nhân sự trước khi khởi hành");
        AppUser staff = users.findById(staffId).orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên"));
        Guard.input(staff.getRole() == Role.DRIVER_ESCORT, "Chỉ phân công tài khoản Tài xế / Người áp tải");
        Guard.state(Boolean.TRUE.equals(staff.getEnabled()), "Tài khoản nhân viên đang bị khóa");
        Guard.state(!assignments.existsByTripIdAndStaffId(t.getId(), staffId), staff.getFullName() + " đã có trong chuyến");
        EmployeeProfile profile = employees.findByUserId(staffId).orElse(null);
        if (role == StaffRole.DRIVER) {
            Guard.state(profile != null && profile.getLicenseNumber() != null,
                    staff.getFullName() + " chưa có thông tin giấy phép lái xe");
            Guard.state(profile.getLicenseExpiry() == null
                            || !profile.getLicenseExpiry().isBefore(t.getPlannedArrival().toLocalDate()),
                    "Giấy phép lái xe của " + staff.getFullName() + " hết hạn trước khi chuyến kết thúc");
        }
        if (role == StaffRole.TEAM_LEADER) {
            boolean hasLeader = assignments.findByTripIdFetch(t.getId()).stream()
                    .anyMatch(a -> a.getAssignmentRole() == StaffRole.TEAM_LEADER);
            Guard.state(!hasLeader, "Chuyến đã có trưởng nhóm");
        }
        Guard.state(!assignments.existsStaffOverlap(staffId, t.getId(), t.getPlannedDeparture(), t.getPlannedArrival()),
                staff.getFullName() + " đã được phân công chuyến khác trùng khung giờ");
        TripStaffAssignment a = new TripStaffAssignment();
        a.setTrip(t);
        a.setStaff(staff);
        a.setAssignmentRole(role);
        a.setAssignedBy(assignedBy);
        a.setAssignedAt(LocalDateTime.now(clock));
        return assignments.save(a);
    }

    private static void requireCoordinator(UserPrincipal actor) {
        if (!actor.isCoordinator()) {
            throw new ForbiddenException("Chỉ Fleet & Route Coordinator lập chuyến và phân công");
        }
    }

    void notifyStaff(TransportationTrip t, NotificationType type, String title, String message) {
        notifications.notifyAll(assignments.findByTripIdFetch(t.getId()).stream().map(TripStaffAssignment::getStaff)
                .toList(), type, title, message, NotificationService.REF_TRIP, t.getId());
    }

    // ================================================================== mapping
    private TripSummary toSummary(TransportationTrip t, List<String> staffNames) {
        return new TripSummary(t.getId(), t.getTripNumber(), t.getRequest().getId(), t.getRequest().getRequestNumber(),
                t.getRequest().getCustomer().getFullName(), t.getRoute().getId(), t.getRoute().getName(),
                t.getRoute().getOriginLocation(), t.getRoute().getDestinationLocation(),
                t.getRoute().getTransportMode(), t.getVehicle() == null ? null : t.getVehicle().getVehicleCode(),
                t.getPlannedDeparture(), t.getPlannedArrival(), t.getActualDeparture(), t.getActualArrival(),
                t.getStatus(), t.getCurrentLocation(), Boolean.TRUE.equals(t.getOperationBlocked()), staffNames);
    }

    public TripDetail toDetail(UserPrincipal actor, TransportationTrip t) {
        TransportationRequest r = t.getRequest();
        List<StaffResponse> staff = assignments.findByTripIdFetch(t.getId()).stream()
                .map(a -> new StaffResponse(a.getId(), a.getStaff().getId(), a.getStaff().getFullName(),
                        a.getStaff().getPhone(), a.getAssignmentRole(), a.getAssignedAt()))
                .toList();
        Map<Long, PreTransportHealthCheck> pre = preChecks.findByTripIdOrderByCheckedAtAsc(t.getId()).stream()
                .collect(Collectors.toMap(c -> c.getHorse().getId(), Function.identity(), (a, b) -> b));
        Map<Long, ArrivalConditionCheck> arr = arrivalChecks.findByTripIdOrderByCheckedAtAsc(t.getId()).stream()
                .collect(Collectors.toMap(c -> c.getHorse().getId(), Function.identity(), (a, b) -> b));
        Map<Long, Handover> hos = handovers.findByTripIdOrderByIdAsc(t.getId()).stream()
                .collect(Collectors.toMap(h -> h.getHorse().getId(), Function.identity(), (a, b) -> b));
        List<TripHorseResponse> horses = new ArrayList<>();
        for (RequestHorse rh : requestHorses.findByRequestIdFetch(r.getId())) {
            Long hid = rh.getHorse().getId();
            PreTransportHealthCheck pc = pre.get(hid);
            ArrivalConditionCheck ac = arr.get(hid);
            Handover ho = hos.get(hid);
            horses.add(new TripHorseResponse(hid, rh.getHorse().getRegisteredName(), rh.getHorse().getMicrochipNumber(),
                    rh.getHorse().getBreed(), rh.getStallType().getName(), rh.getStallType().getWidthCm(),
                    rh.getHorse().getSpecialRequirements(), pc != null,
                    pc == null ? null : pc.getFitForTravelObserved(), ac != null,
                    ac == null ? null : ac.getHorseCondition(), ho == null ? null : ho.getId(),
                    ho == null ? null : ho.getStatus()));
        }
        List<IncidentRef> incidentRefs = incidents.findByTripIdOrderByReportedAtDesc(t.getId()).stream()
                .map(i -> new IncidentRef(i.getId(), i.getIncidentNumber(), i.getTitle(), i.getSeverity(),
                        i.getStatus(), i.getReportedAt()))
                .toList();
        Vehicle v = t.getVehicle();
        VehicleRef vehicleRef = v == null ? null : new VehicleRef(v.getId(), v.getVehicleCode(), v.getPlateNumber(),
                v.getVehicleType(), v.getCapacityHorses(), Boolean.TRUE.equals(v.getClimateControlled()));
        long delay = trackingEvents.sumDelay(t.getId(), TrackingEventType.DELAY);
        return new TripDetail(t.getId(), t.getTripNumber(), r.getId(), r.getRequestNumber(), r.getCustomer().getId(),
                r.getCustomer().getFullName(), actor.isCustomer() ? null : r.getCustomer().getPhone(),
                t.getRoute().getId(), t.getRoute().getRouteCode(), t.getRoute().getName(),
                t.getRoute().getTransportMode(), t.getRoute().getOriginLocation(), t.getRoute().getDestinationLocation(),
                r.getDestinationAddress(), t.getPlannedDeparture(), t.getPlannedArrival(), t.getActualDeparture(),
                t.getActualArrival(), t.getStatus(), t.getCurrentLocation(),
                t.getCurrentStop() == null ? null : t.getCurrentStop().getId(),
                Boolean.TRUE.equals(t.getOperationBlocked()), t.getTransportInstructions(), t.getNotes(),
                t.getCoordinator().getFullName(), vehicleRef, staff, horses,
                routeStops.findByRouteIdOrderByStopOrderAsc(t.getRoute().getId()).stream().map(RouteService::toStop).toList(),
                incidentRefs, delay, nextStatuses(actor, t), allowedActions(actor, t, horses, staff), t.getCreatedAt());
    }

    /** Trạng thái Driver/Escort có thể chọn thủ công trong màn hình cập nhật. */
    private List<TripStatus> nextStatuses(UserPrincipal actor, TransportationTrip t) {
        if (!actor.isDriverEscort() && !actor.isManager()) {
            return List.of();
        }
        if (!TripStateMachine.EN_ROUTE.contains(t.getStatus())) {
            return List.of();
        }
        if (Boolean.TRUE.equals(t.getOperationBlocked())) {
            return t.getStatus() == TripStatus.DELAYED ? List.of() : List.of(TripStatus.DELAYED);
        }
        return TripStateMachine.targets(t.getStatus()).stream().sorted().toList();
    }

    private List<String> allowedActions(UserPrincipal actor, TransportationTrip t, List<TripHorseResponse> horses,
                                        List<StaffResponse> staff) {
        List<String> a = new ArrayList<>();
        TripStatus s = t.getStatus();
        boolean pre = TripStateMachine.PRE_DEPARTURE.contains(s);
        if (actor.isCoordinator() && pre) {
            a.add("ASSIGN_VEHICLE");
            a.add("ASSIGN_STAFF");
            a.add("EDIT_PLAN");
            a.add("CANCEL");
        }
        if (actor.isManager() && pre) {
            a.add("CANCEL");
        }
        boolean assigned = actor.isDriverEscort() && staff.stream().anyMatch(x -> x.staffId().equals(actor.getId()));
        if (assigned) {
            if (pre) {
                a.add("PRE_CHECK");
            }
            boolean allFit = !horses.isEmpty() && horses.stream().allMatch(h -> Boolean.TRUE.equals(h.fitForTravelObserved()));
            if (s == TripStatus.READY_FOR_DEPARTURE && allFit && t.getVehicle() != null
                    && !Boolean.TRUE.equals(t.getOperationBlocked())) {
                a.add("START");
            }
            if (TripStateMachine.EN_ROUTE.contains(s)) {
                a.add("CHECKPOINT");
                a.add("UPDATE_STATUS");
                a.add("HEALTH_OBSERVATION");
                a.add("REPORT_INCIDENT");
            }
            if (s == TripStatus.ARRIVED || s == TripStatus.HANDOVER_PENDING) {
                a.add("ARRIVAL_CHECK");
                a.add("HANDOVER");
                a.add("HEALTH_OBSERVATION");
                a.add("REPORT_INCIDENT");
            }
            if (s == TripStatus.PREPARING || s == TripStatus.READY_FOR_DEPARTURE) {
                a.add("REPORT_INCIDENT");
            }
        }
        if (actor.isManager() && Boolean.TRUE.equals(t.getOperationBlocked())) {
            a.add("DECIDE_INCIDENT");
        }
        return a;
    }

    // ================================================================== lưu trữ (xóa mềm)
    private static final EnumSet<TripStatus> FINISHED = EnumSet.of(TripStatus.DELIVERED, TripStatus.CANCELLED);

    /** Manager lưu trữ một chuyến đã bàn giao / đã hủy: ẩn khỏi danh sách, báo cáo và lịch sử vẫn giữ nguyên. */
    @Transactional
    public void archive(UserPrincipal actor, Long tripId) {
        if (!actor.isManager()) {
            throw new ForbiddenException("Chỉ Logistics Manager được xóa (lưu trữ) chuyến");
        }
        TransportationTrip t = tripAccess.load(tripId);
        Guard.state(FINISHED.contains(t.getStatus()), "Chỉ xóa được chuyến đã bàn giao hoặc đã hủy");
        Guard.state(!Boolean.TRUE.equals(t.getArchived()), "Chuyến đã được lưu trữ");
        t.setArchived(true);
        t.setArchivedAt(LocalDateTime.now(clock));
    }

    /** Dọn hàng loạt các chuyến đã bàn giao / đã hủy cập nhật lần cuối cách đây ít nhất {@code olderThanDays} ngày. */
    @Transactional
    public int archiveFinished(UserPrincipal actor, int olderThanDays) {
        if (!actor.isManager()) {
            throw new ForbiddenException("Chỉ Logistics Manager được dọn chuyến đã hoàn tất");
        }
        LocalDateTime limit = LocalDateTime.now(clock).minusDays(Math.max(olderThanDays, 0));
        LocalDateTime now = LocalDateTime.now(clock);
        int n = 0;
        for (TransportationTrip t : trips.findByStatusInAndArchivedFalse(FINISHED)) {
            if (t.getUpdatedAt() == null || !t.getUpdatedAt().isAfter(limit)) {
                t.setArchived(true);
                t.setArchivedAt(now);
                n++;
            }
        }
        return n;
    }
}
