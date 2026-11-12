package com.example.racetransport.handover.service;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.document.entity.AttachmentRefType;
import com.example.racetransport.document.service.AttachmentService;
import com.example.racetransport.fleet.entity.Vehicle;
import com.example.racetransport.fleet.entity.VehicleStatus;
import com.example.racetransport.handover.dto.HandoverDtos.ConfirmHandoverRequest;
import com.example.racetransport.handover.dto.HandoverDtos.HandoverRequest;
import com.example.racetransport.handover.dto.HandoverDtos.HandoverResponse;
import com.example.racetransport.handover.entity.Handover;
import com.example.racetransport.handover.entity.HandoverStatus;
import com.example.racetransport.handover.repository.HandoverRepository;
import com.example.racetransport.health.entity.ArrivalConditionCheck;
import com.example.racetransport.health.repository.ArrivalConditionCheckRepository;
import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.horse.entity.HorseStatus;
import com.example.racetransport.horse.repository.HorseRepository;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.tracking.service.TripStateMachine;
import com.example.racetransport.tracking.service.TripStatusService;
import com.example.racetransport.transportationrequest.entity.RequestHorse;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.transportationrequest.service.RequestWorkflow;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStaffAssignment;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.trip.service.TripAccess;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Arrival & Handover: Driver/Escort lập biên bản bàn giao từng ngựa (sau kiểm tra khi đến),
 * khách hàng xác nhận. Chuyến chỉ DELIVERED khi mọi ngựa đã được xác nhận bàn giao;
 * yêu cầu COMPLETED khi mọi chuyến của yêu cầu đã giao/hủy.
 */
@Service
public class HandoverService {

    private final HandoverRepository handovers;
    private final ArrivalConditionCheckRepository arrivalChecks;
    private final HorseRepository horses;
    private final RequestHorseRepository requestHorses;
    private final TransportationTripRepository trips;
    private final TripStaffAssignmentRepository assignments;
    private final TripAccess tripAccess;
    private final TripStatusService statusService;
    private final RequestWorkflow requestWorkflow;
    private final AttachmentService attachmentService;
    private final NotificationService notifications;
    private final AppUserRepository users;
    private final CodeGenerator codes;
    private final Clock clock;

    public HandoverService(HandoverRepository handovers, ArrivalConditionCheckRepository arrivalChecks,
                           HorseRepository horses, RequestHorseRepository requestHorses,
                           TransportationTripRepository trips, TripStaffAssignmentRepository assignments,
                           TripAccess tripAccess, TripStatusService statusService, RequestWorkflow requestWorkflow,
                           AttachmentService attachmentService, NotificationService notifications,
                           AppUserRepository users, CodeGenerator codes, Clock clock) {
        this.handovers = handovers;
        this.arrivalChecks = arrivalChecks;
        this.horses = horses;
        this.requestHorses = requestHorses;
        this.trips = trips;
        this.assignments = assignments;
        this.tripAccess = tripAccess;
        this.statusService = statusService;
        this.requestWorkflow = requestWorkflow;
        this.attachmentService = attachmentService;
        this.notifications = notifications;
        this.users = users;
        this.codes = codes;
        this.clock = clock;
    }

    @Transactional
    public HandoverResponse create(UserPrincipal actor, HandoverRequest req) {
        TransportationTrip t = tripAccess.loadAssigned(actor, req.tripId());
        Guard.state(t.getStatus() == TripStatus.ARRIVED || t.getStatus() == TripStatus.HANDOVER_PENDING,
                "Chỉ lập biên bản bàn giao khi chuyến đã đến nơi");
        Horse horse = horses.findById(req.horseId()).orElseThrow(() -> new NotFoundException("Không tìm thấy ngựa"));
        Guard.input(requestHorses.existsByRequestIdAndHorseId(t.getRequest().getId(), horse.getId()), "Ngựa không thuộc chuyến này");
        Guard.state(handovers.findByTripIdAndHorseId(t.getId(), horse.getId()).isEmpty(),
                "Đã có biên bản bàn giao cho " + horse.getRegisteredName() + "; hãy cập nhật biên bản hiện có");
        ArrivalConditionCheck check = arrivalChecks.findByTripIdAndHorseId(t.getId(), horse.getId())
                .orElseThrow(() -> new com.example.racetransport.common.exception.InvalidStateException(
                        "Cần kiểm tra tình trạng khi đến cho " + horse.getRegisteredName() + " trước khi bàn giao"));
        AppUser user = users.getReferenceById(actor.getId());
        Handover h = new Handover();
        h.setHandoverNumber(codes.next(CodeGenerator.HANDOVER));
        h.setTrip(t);
        h.setHorse(horse);
        h.setArrivalCheck(check);
        h.setCreatedBy(user);
        apply(h, req, t);
        h.setStatus(HandoverStatus.PENDING_CONFIRMATION);
        handovers.save(h);
        if (t.getStatus() == TripStatus.ARRIVED) {
            statusService.changeStatus(t, TripStatus.HANDOVER_PENDING, user, "Lập biên bản bàn giao " + h.getHandoverNumber());
        }
        notifications.notify(t.getRequest().getCustomer(), NotificationType.ACTION_REQUIRED,
                "Xác nhận bàn giao " + horse.getRegisteredName(),
                "Biên bản " + h.getHandoverNumber() + " – người nhận " + h.getReceiverName() + ". Vui lòng xác nhận.",
                NotificationService.REF_HANDOVER, h.getId());
        return toResponse(actor, h);
    }

    @Transactional
    public HandoverResponse update(UserPrincipal actor, Long id, HandoverRequest req) {
        Handover h = load(id);
        TransportationTrip t = h.getTrip();
        if (!actor.isDriverEscort() || !tripAccess.isAssigned(actor, t)) {
            throw new ForbiddenException("Chỉ Driver/Escort của chuyến được sửa biên bản");
        }
        Guard.state(h.getStatus() != HandoverStatus.CONFIRMED, "Biên bản đã được khách hàng xác nhận");
        Guard.input(req.tripId().equals(t.getId()) && req.horseId().equals(h.getHorse().getId()),
                "Không đổi chuyến/ngựa của biên bản");
        apply(h, req, t);
        if (h.getStatus() == HandoverStatus.DISPUTED) {
            h.setStatus(HandoverStatus.PENDING_CONFIRMATION);
            notifications.notify(t.getRequest().getCustomer(), NotificationType.ACTION_REQUIRED,
                    "Biên bản bàn giao đã được cập nhật", h.getHandoverNumber() + " – vui lòng xác nhận lại.",
                    NotificationService.REF_HANDOVER, h.getId());
        }
        return toResponse(actor, h);
    }

    /** Khách hàng xác nhận: chỉ khi biên bản đang chờ xác nhận. */
    @Transactional
    public HandoverResponse confirm(UserPrincipal actor, Long id, ConfirmHandoverRequest req) {
        Handover h = load(id);
        TransportationTrip t = h.getTrip();
        TransportationRequest r = t.getRequest();
        if (!actor.isCustomer() || !r.getCustomer().getId().equals(actor.getId())) {
            throw new ForbiddenException("Chỉ khách hàng sở hữu yêu cầu được xác nhận bàn giao");
        }
        Guard.state(h.getStatus() == HandoverStatus.PENDING_CONFIRMATION,
                "Biên bản ở trạng thái \"" + h.getStatus().getLabel() + "\" không thể xác nhận");
        Guard.state(t.getStatus() == TripStatus.HANDOVER_PENDING, "Chuyến không ở trạng thái chờ bàn giao");
        AppUser customer = users.getReferenceById(actor.getId());
        LocalDateTime now = LocalDateTime.now(clock);
        h.setCustomerComment(req.comment());
        List<AppUser> staff = assignments.findByTripIdFetch(t.getId()).stream().map(TripStaffAssignment::getStaff).toList();
        if (!Boolean.TRUE.equals(req.accepted())) {
            Guard.input(req.comment() != null && !req.comment().isBlank(), "Vui lòng mô tả vấn đề khi nhận ngựa");
            h.setStatus(HandoverStatus.DISPUTED);
            notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.WARNING,
                    "Khách hàng phản hồi vấn đề bàn giao " + h.getHandoverNumber(), req.comment(),
                    NotificationService.REF_HANDOVER, h.getId());
            notifications.notifyAll(staff, NotificationType.ACTION_REQUIRED,
                    "Biên bản " + h.getHandoverNumber() + " cần điều chỉnh", req.comment(), NotificationService.REF_HANDOVER, h.getId());
            return toResponse(actor, h);
        }
        h.setStatus(HandoverStatus.CONFIRMED);
        h.setConfirmedBy(customer);
        h.setConfirmedAt(now);
        h.getHorse().setStatus(HorseStatus.ACTIVE);

        // Hoàn tất chuyến khi tất cả ngựa đã được xác nhận bàn giao
        List<RequestHorse> all = requestHorses.findByRequestIdFetch(r.getId());
        long confirmed = handovers.countByTripIdAndStatus(t.getId(), HandoverStatus.CONFIRMED);
        if (confirmed >= all.size()) {
            statusService.changeStatus(t, TripStatus.DELIVERED, customer, "Khách hàng xác nhận bàn giao đủ " + all.size() + " ngựa");
            if (t.getActualArrival() == null) {
                t.setActualArrival(now);
            }
            Vehicle v = t.getVehicle();
            if (v != null && v.getStatus() == VehicleStatus.IN_USE) {
                boolean otherRunning = trips.findByVehicleIdOrderByPlannedDepartureDesc(v.getId()).stream()
                        .anyMatch(x -> !x.getId().equals(t.getId()) && TripStateMachine.EN_ROUTE.contains(x.getStatus()));
                if (!otherRunning) {
                    v.setStatus(VehicleStatus.AVAILABLE);
                    v.setCurrentLocation(t.getRoute().getDestinationLocation());
                }
            }
            boolean allDone = trips.findByRequestIdOrderByPlannedDepartureAsc(r.getId()).stream()
                    .allMatch(x -> x.getStatus() == TripStatus.DELIVERED || x.getStatus() == TripStatus.CANCELLED);
            if (allDone && r.getStatus() == RequestStatus.TRIP_PLANNED) {
                requestWorkflow.transition(r, RequestStatus.COMPLETED, customer, "Hoàn tất bàn giao");
            }
            notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.SUCCESS,
                    "Chuyến " + t.getTripNumber() + " đã giao thành công", r.getRequestNumber(), NotificationService.REF_TRIP, t.getId());
            notifications.notifyAll(staff, NotificationType.SUCCESS, "Chuyến " + t.getTripNumber() + " đã hoàn tất bàn giao",
                    "Cảm ơn đội vận hành.", NotificationService.REF_TRIP, t.getId());
        }
        return toResponse(actor, h);
    }

    @Transactional(readOnly = true)
    public HandoverResponse get(UserPrincipal actor, Long id) {
        Handover h = load(id);
        if (!tripAccess.canView(actor, h.getTrip())) {
            throw new ForbiddenException("Bạn không có quyền xem biên bản này");
        }
        return toResponse(actor, h);
    }

    @Transactional(readOnly = true)
    public List<HandoverResponse> forTrip(UserPrincipal actor, Long tripId) {
        TransportationTrip t = tripAccess.loadVisible(actor, tripId);
        return handovers.findByTripIdOrderByIdAsc(t.getId()).stream().map(h -> toResponse(actor, h)).toList();
    }

    @Transactional(readOnly = true)
    public List<HandoverResponse> list(UserPrincipal actor, List<HandoverStatus> statuses) {
        if (actor.isSpecialist() || actor.isCoordinator()) {
            throw new ForbiddenException("Không có quyền xem danh sách bàn giao");
        }
        return handovers.search(statuses == null || statuses.isEmpty() ? EnumSet.allOf(HandoverStatus.class) : statuses,
                        actor.isCustomer() ? actor.getId() : null, actor.isDriverEscort() ? actor.getId() : null)
                .stream().map(h -> toResponse(actor, h)).toList();
    }

    public Handover load(Long id) {
        return handovers.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy biên bản bàn giao"));
    }

    private void apply(Handover h, HandoverRequest req, TransportationTrip t) {
        h.setDestination(req.destination() == null || req.destination().isBlank()
                ? t.getRequest().getDestinationAddress() : req.destination());
        LocalDateTime now = LocalDateTime.now(clock);
        h.setHandoverTime(req.handoverTime() == null || req.handoverTime().isAfter(now) ? now : req.handoverTime());
        h.setReceiverName(req.receiverName().trim());
        h.setReceiverContact(req.receiverContact());
        h.setHorseCondition(req.condition());
        h.setDocumentsHandedOver(Boolean.TRUE.equals(req.documentsHandedOver()));
        h.setNotes(req.notes());
    }

    private HandoverResponse toResponse(UserPrincipal actor, Handover h) {
        TransportationTrip t = h.getTrip();
        List<String> actions = new ArrayList<>();
        if (actor.isCustomer() && t.getRequest().getCustomer().getId().equals(actor.getId())
                && h.getStatus() == HandoverStatus.PENDING_CONFIRMATION) {
            actions.add("CONFIRM");
            actions.add("DISPUTE");
        }
        if (actor.isDriverEscort() && tripAccess.isAssigned(actor, t) && h.getStatus() != HandoverStatus.CONFIRMED) {
            actions.add("EDIT");
            actions.add("ADD_EVIDENCE");
        }
        ArrivalConditionCheck ac = h.getArrivalCheck();
        return new HandoverResponse(h.getId(), h.getHandoverNumber(), t.getId(), t.getTripNumber(), t.getStatus(),
                t.getRequest().getId(), t.getRequest().getRequestNumber(), h.getHorse().getId(),
                h.getHorse().getRegisteredName(), h.getHorse().getMicrochipNumber(), h.getDestination(),
                h.getHandoverTime(), h.getReceiverName(), h.getReceiverContact(), h.getHorseCondition(),
                Boolean.TRUE.equals(h.getDocumentsHandedOver()), h.getNotes(), h.getStatus(),
                ac == null ? null : ac.getId(), ac == null ? null : ac.getHorseCondition(),
                h.getCreatedBy().getFullName(), h.getConfirmedBy() == null ? null : h.getConfirmedBy().getFullName(),
                h.getConfirmedAt(), h.getCustomerComment(), attachmentService.refs(AttachmentRefType.HANDOVER, h.getId()),
                actions, h.getUpdatedAt());
    }
}
