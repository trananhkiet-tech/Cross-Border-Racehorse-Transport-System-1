package com.example.racetransport.trip.service;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import org.springframework.stereotype.Component;

/**
 * Quyền truy cập chuyến:
 * - CUSTOMER: chuyến thuộc yêu cầu của mình (chỉ xem).
 * - DRIVER_ESCORT: chỉ chuyến được phân công (xem + thao tác thực thi).
 * - Manager / Specialist / Coordinator: xem mọi chuyến.
 */
@Component
public class TripAccess {

    private final TransportationTripRepository trips;
    private final TripStaffAssignmentRepository assignments;

    public TripAccess(TransportationTripRepository trips, TripStaffAssignmentRepository assignments) {
        this.trips = trips;
        this.assignments = assignments;
    }

    public TransportationTrip load(Long id) {
        return trips.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy chuyến"));
    }

    public TransportationTrip loadVisible(UserPrincipal actor, Long id) {
        TransportationTrip t = load(id);
        if (!canView(actor, t)) {
            throw new ForbiddenException("Bạn không có quyền xem chuyến này");
        }
        return t;
    }

    /** Driver/Escort phải được phân công vào chuyến. */
    public TransportationTrip loadAssigned(UserPrincipal actor, Long id) {
        TransportationTrip t = load(id);
        if (!actor.isDriverEscort() || !isAssigned(actor, t)) {
            throw new ForbiddenException("Bạn không được phân công cho chuyến này");
        }
        return t;
    }

    public boolean isAssigned(UserPrincipal actor, TransportationTrip t) {
        return assignments.existsByTripIdAndStaffId(t.getId(), actor.getId());
    }

    public boolean canView(UserPrincipal actor, TransportationTrip t) {
        if (actor.isCustomer()) {
            return t.getRequest().getCustomer().getId().equals(actor.getId());
        }
        if (actor.isDriverEscort()) {
            return isAssigned(actor, t);
        }
        return true;
    }
}
