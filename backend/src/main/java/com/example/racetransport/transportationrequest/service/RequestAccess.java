package com.example.racetransport.transportationrequest.service;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.TransportationRequestRepository;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra quyền truy cập một Transportation Request (dùng chung cho nhiều module).
 * - CUSTOMER: chỉ yêu cầu của chính mình.
 * - DRIVER_ESCORT: chỉ yêu cầu có chuyến mình được phân công.
 * - Manager / Specialist / Coordinator: mọi yêu cầu.
 */
@Component
public class RequestAccess {

    private final TransportationRequestRepository requests;
    private final TripStaffAssignmentRepository assignments;

    public RequestAccess(TransportationRequestRepository requests, TripStaffAssignmentRepository assignments) {
        this.requests = requests;
        this.assignments = assignments;
    }

    public TransportationRequest load(Long id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu vận chuyển"));
    }

    public TransportationRequest loadVisible(UserPrincipal actor, Long id) {
        TransportationRequest r = load(id);
        if (!canView(actor, r)) {
            throw new ForbiddenException("Bạn không có quyền truy cập yêu cầu này");
        }
        return r;
    }

    /** Chỉ chủ sở hữu (khách hàng) của yêu cầu. */
    public TransportationRequest loadOwned(UserPrincipal actor, Long id) {
        TransportationRequest r = load(id);
        if (!isOwner(actor, r)) {
            throw new ForbiddenException("Chỉ khách hàng sở hữu yêu cầu mới được thực hiện thao tác này");
        }
        return r;
    }

    public boolean isOwner(UserPrincipal actor, TransportationRequest r) {
        return actor.isCustomer() && r.getCustomer().getId().equals(actor.getId());
    }

    public boolean canView(UserPrincipal actor, TransportationRequest r) {
        if (actor.isCustomer()) {
            return r.getCustomer().getId().equals(actor.getId());
        }
        if (actor.isDriverEscort()) {
            return assignments.existsByTripRequestIdAndStaffId(r.getId(), actor.getId());
        }
        return true;
    }
}
