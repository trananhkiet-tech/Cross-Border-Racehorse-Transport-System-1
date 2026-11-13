package com.example.racetransport.staffleave.controller;

import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.staffleave.dto.LeaveDtos.LeaveRequest;
import com.example.racetransport.staffleave.dto.LeaveDtos.LeaveResponse;
import com.example.racetransport.staffleave.service.StaffLeaveService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff-leaves")
@Tag(name = "Staff leave")
public class StaffLeaveController {

    private final StaffLeaveService service;

    public StaffLeaveController(StaffLeaveService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','FLEET_ROUTE_COORDINATOR')")
    public List<LeaveResponse> upcoming() {
        return service.upcoming();
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('DRIVER_ESCORT')")
    public List<LeaveResponse> mine(@AuthenticationPrincipal UserPrincipal actor) {
        return service.mine(actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','FLEET_ROUTE_COORDINATOR')")
    public LeaveResponse create(@AuthenticationPrincipal UserPrincipal actor, @Valid @RequestBody LeaveRequest req) {
        return service.create(actor, req);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','FLEET_ROUTE_COORDINATOR')")
    public LeaveResponse cancel(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return service.cancel(actor, id);
    }
}
