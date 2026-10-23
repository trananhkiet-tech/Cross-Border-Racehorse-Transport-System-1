package com.example.racetransport.compliance.controller;

import com.example.racetransport.compliance.dto.ComplianceDtos.ChecklistItemResponse;
import com.example.racetransport.compliance.dto.ComplianceDtos.ChecklistResponse;
import com.example.racetransport.compliance.dto.ComplianceDtos.OpenItemResponse;
import com.example.racetransport.compliance.dto.ComplianceDtos.PatchChecklistItemRequest;
import com.example.racetransport.compliance.dto.ComplianceDtos.RequirementRequest;
import com.example.racetransport.compliance.dto.ComplianceDtos.RequirementResponse;
import com.example.racetransport.compliance.entity.ComplianceCategory;
import com.example.racetransport.compliance.service.ComplianceService;
import com.example.racetransport.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/compliance")
@Tag(name = "Compliance")
public class ComplianceController {

    private final ComplianceService service;

    public ComplianceController(ComplianceService service) {
        this.service = service;
    }

    @GetMapping("/requirements")
    @Operation(summary = "Danh mục yêu cầu tuân thủ DEMO (lọc theo nhóm và nước đến)")
    public List<RequirementResponse> requirements(@RequestParam(required = false) List<ComplianceCategory> category,
                                                  @RequestParam(required = false) String destination,
                                                  @RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.listRequirements(category, destination, includeInactive);
    }

    @PostMapping("/requirements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TRANSPORT_SPECIALIST')")
    public RequirementResponse createRequirement(@AuthenticationPrincipal UserPrincipal actor,
                                                 @Valid @RequestBody RequirementRequest req) {
        return service.createRequirement(actor, req);
    }

    @PutMapping("/requirements/{id}")
    @PreAuthorize("hasRole('TRANSPORT_SPECIALIST')")
    public RequirementResponse updateRequirement(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                                 @Valid @RequestBody RequirementRequest req) {
        return service.updateRequirement(actor, id, req);
    }

    @GetMapping("/checklists/{requestId}")
    public ChecklistResponse checklist(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long requestId) {
        return service.getChecklist(actor, requestId);
    }

    @PostMapping("/checklists/{requestId}/generate")
    @PreAuthorize("hasAnyRole('TRANSPORT_SPECIALIST','LOGISTICS_MANAGER')")
    public ChecklistResponse generate(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long requestId) {
        return service.regenerate(actor, requestId);
    }

    @PatchMapping("/checklists/{itemId}")
    @PreAuthorize("hasRole('TRANSPORT_SPECIALIST')")
    public ChecklistItemResponse patch(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long itemId,
                                       @Valid @RequestBody PatchChecklistItemRequest req) {
        return service.patchItem(actor, itemId, req);
    }

    @PostMapping("/checklists/{requestId}/notify-missing")
    @PreAuthorize("hasRole('TRANSPORT_SPECIALIST')")
    public Map<String, Integer> notifyMissing(@AuthenticationPrincipal UserPrincipal actor,
                                              @PathVariable Long requestId) {
        return Map.of("items", service.notifyMissing(actor, requestId));
    }

    @GetMapping("/open-items")
    @PreAuthorize("hasAnyRole('CUSTOMER','TRANSPORT_SPECIALIST','LOGISTICS_MANAGER')")
    @Operation(summary = "Tài liệu còn thiếu / cần cập nhật (Missing Documents)")
    public List<OpenItemResponse> openItems(@AuthenticationPrincipal UserPrincipal actor) {
        return service.openItems(actor);
    }
}
