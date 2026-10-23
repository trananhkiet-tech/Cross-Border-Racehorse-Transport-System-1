package com.example.racetransport.trip.controller;

import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.trip.dto.TripDtos.AssignStaffRequest;
import com.example.racetransport.trip.dto.TripDtos.AssignVehicleRequest;
import com.example.racetransport.trip.dto.TripDtos.CancelTripRequest;
import com.example.racetransport.trip.dto.TripDtos.CheckpointRequest;
import com.example.racetransport.trip.dto.TripDtos.CreateTripRequest;
import com.example.racetransport.trip.dto.TripDtos.StatusUpdateRequest;
import com.example.racetransport.trip.dto.TripDtos.TimelineItem;
import com.example.racetransport.trip.dto.TripDtos.TrackingEventResponse;
import com.example.racetransport.trip.dto.TripDtos.TrackingResponse;
import com.example.racetransport.trip.dto.TripDtos.TripDetail;
import com.example.racetransport.trip.dto.TripDtos.TripSummary;
import com.example.racetransport.trip.dto.TripDtos.UpdateTripPlanRequest;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.service.TripExecutionService;
import com.example.racetransport.trip.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
@Tag(name = "Trips & Tracking")
public class TripController {

    private final TripService tripService;
    private final TripExecutionService execution;

    public TripController(TripService tripService, TripExecutionService execution) {
        this.tripService = tripService;
        this.execution = execution;
    }

    @GetMapping
    @Operation(summary = "Customer: chuyến của mình; Driver/Escort: chuyến được giao; nhân viên khác: tất cả")
    public PageResponse<TripSummary> list(@AuthenticationPrincipal UserPrincipal actor,
                                          @RequestParam(required = false) List<TripStatus> status,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return tripService.list(actor, status, q, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public TripDetail create(@AuthenticationPrincipal UserPrincipal actor, @Valid @RequestBody CreateTripRequest req) {
        return tripService.create(actor, req);
    }

    @GetMapping("/{id}")
    public TripDetail get(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return tripService.get(actor, id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public TripDetail updatePlan(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                 @Valid @RequestBody UpdateTripPlanRequest req) {
        return tripService.updatePlan(actor, id, req);
    }

    @PostMapping("/{id}/assign-vehicle")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public TripDetail assignVehicle(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                    @Valid @RequestBody AssignVehicleRequest req) {
        return tripService.assignVehicle(actor, id, req.vehicleId());
    }

    @PostMapping("/{id}/assign-staff")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public TripDetail assignStaff(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                  @Valid @RequestBody AssignStaffRequest req) {
        return tripService.assignStaff(actor, id, req.staffId(), req.role());
    }

    @DeleteMapping("/{id}/staff/{staffId}")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public TripDetail unassignStaff(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                    @PathVariable Long staffId) {
        return tripService.unassignStaff(actor, id, staffId);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('FLEET_ROUTE_COORDINATOR','LOGISTICS_MANAGER')")
    public TripDetail cancel(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                             @Valid @RequestBody CancelTripRequest req) {
        return tripService.cancel(actor, id, req.note());
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('DRIVER_ESCORT')")
    public TripDetail start(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return execution.start(actor, id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DRIVER_ESCORT','LOGISTICS_MANAGER')")
    public TripDetail updateStatus(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                   @Valid @RequestBody StatusUpdateRequest req) {
        return execution.updateStatus(actor, id, req);
    }

    @PostMapping("/{id}/checkpoints")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DRIVER_ESCORT')")
    public TrackingEventResponse checkpoint(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                            @Valid @RequestBody CheckpointRequest req) {
        return execution.checkpoint(actor, id, req);
    }

    @GetMapping("/{id}/tracking")
    public TrackingResponse tracking(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return execution.tracking(actor, id);
    }

    @GetMapping("/{id}/timeline")
    public List<TimelineItem> timeline(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return execution.timeline(actor, id);
    }
}
