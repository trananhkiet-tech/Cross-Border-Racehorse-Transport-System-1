package com.example.racetransport.handover.controller;

import com.example.racetransport.handover.dto.HandoverDtos.ConfirmHandoverRequest;
import com.example.racetransport.handover.dto.HandoverDtos.HandoverRequest;
import com.example.racetransport.handover.dto.HandoverDtos.HandoverResponse;
import com.example.racetransport.handover.entity.HandoverStatus;
import com.example.racetransport.handover.service.HandoverService;
import com.example.racetransport.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Handover")
public class HandoverController {

    private final HandoverService service;

    public HandoverController(HandoverService service) {
        this.service = service;
    }

    @PostMapping("/api/handovers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DRIVER_ESCORT')")
    public HandoverResponse create(@AuthenticationPrincipal UserPrincipal actor, @Valid @RequestBody HandoverRequest req) {
        return service.create(actor, req);
    }

    @GetMapping("/api/handovers")
    public List<HandoverResponse> list(@AuthenticationPrincipal UserPrincipal actor,
                                       @RequestParam(required = false) List<HandoverStatus> status) {
        return service.list(actor, status);
    }

    @GetMapping("/api/handovers/{id}")
    public HandoverResponse get(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return service.get(actor, id);
    }

    @PutMapping("/api/handovers/{id}")
    @PreAuthorize("hasRole('DRIVER_ESCORT')")
    public HandoverResponse update(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                   @Valid @RequestBody HandoverRequest req) {
        return service.update(actor, id, req);
    }

    @PostMapping("/api/handovers/{id}/confirm")
    @PreAuthorize("hasRole('CUSTOMER')")
    public HandoverResponse confirm(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                    @Valid @RequestBody ConfirmHandoverRequest req) {
        return service.confirm(actor, id, req);
    }

    @GetMapping("/api/trips/{tripId}/handovers")
    public List<HandoverResponse> forTrip(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long tripId) {
        return service.forTrip(actor, tripId);
    }
}
