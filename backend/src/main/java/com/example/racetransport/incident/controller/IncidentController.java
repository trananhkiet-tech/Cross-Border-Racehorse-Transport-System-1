package com.example.racetransport.incident.controller;

import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.incident.dto.IncidentDtos.IncidentActionRequest;
import com.example.racetransport.incident.dto.IncidentDtos.IncidentDetail;
import com.example.racetransport.incident.dto.IncidentDtos.IncidentStatusRequest;
import com.example.racetransport.incident.dto.IncidentDtos.IncidentSummary;
import com.example.racetransport.incident.dto.IncidentDtos.ReportIncidentRequest;
import com.example.racetransport.incident.entity.IncidentSeverity;
import com.example.racetransport.incident.entity.IncidentStatus;
import com.example.racetransport.incident.service.IncidentService;
import com.example.racetransport.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
@Tag(name = "Incidents & Emergency")
public class IncidentController {

    private final IncidentService service;

    public IncidentController(IncidentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DRIVER_ESCORT','LOGISTICS_MANAGER')")
    @Operation(summary = "Báo sự cố. Mức Cao/Nghiêm trọng tự tạm dừng chuyến chờ Manager quyết định")
    public IncidentDetail report(@AuthenticationPrincipal UserPrincipal actor, @Valid @RequestBody ReportIncidentRequest req) {
        return service.report(actor, req);
    }

    @GetMapping
    public PageResponse<IncidentSummary> list(@AuthenticationPrincipal UserPrincipal actor,
                                              @RequestParam(required = false) List<IncidentStatus> status,
                                              @RequestParam(required = false) List<IncidentSeverity> severity,
                                              @RequestParam(required = false) Long tripId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.list(actor, status, severity, tripId, page, size);
    }

    @GetMapping("/{id}")
    public IncidentDetail get(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return service.get(actor, id);
    }

    @PostMapping("/{id}/actions")
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','TRANSPORT_SPECIALIST','DRIVER_ESCORT')")
    @Operation(summary = "Ghi hành động xử lý; actionType=DECISION (chỉ Manager) với Continue/Monitor/Stop/Reroute")
    public IncidentDetail addAction(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                    @Valid @RequestBody IncidentActionRequest req) {
        return service.addAction(actor, id, req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public IncidentDetail status(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                 @Valid @RequestBody IncidentStatusRequest req) {
        return service.changeStatus(actor, id, req.status(), req.note());
    }
}
