package com.example.racetransport.claim.controller;

import com.example.racetransport.claim.dto.ClaimDtos.ApproveClaimRequest;
import com.example.racetransport.claim.dto.ClaimDtos.ClaimDetail;
import com.example.racetransport.claim.dto.ClaimDtos.ClaimSummary;
import com.example.racetransport.claim.dto.ClaimDtos.CloseClaimRequest;
import com.example.racetransport.claim.dto.ClaimDtos.CreateClaimRequest;
import com.example.racetransport.claim.dto.ClaimDtos.RejectClaimRequest;
import com.example.racetransport.claim.dto.ClaimDtos.ReviewClaimRequest;
import com.example.racetransport.claim.dto.ClaimDtos.SettlementRequest;
import com.example.racetransport.claim.entity.ClaimStatus;
import com.example.racetransport.claim.entity.EvidenceType;
import com.example.racetransport.claim.service.ClaimService;
import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.document.controller.FileResponses;
import com.example.racetransport.document.entity.StoredFile;
import com.example.racetransport.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/claims")
@Tag(name = "Claims & Settlement")
@PreAuthorize("hasAnyRole('CUSTOMER','LOGISTICS_MANAGER')")
public class ClaimController {

    private final ClaimService service;

    public ClaimController(ClaimService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ClaimDetail create(@AuthenticationPrincipal UserPrincipal actor, @Valid @RequestBody CreateClaimRequest req) {
        return service.create(actor, req);
    }

    @GetMapping
    public PageResponse<ClaimSummary> list(@AuthenticationPrincipal UserPrincipal actor,
                                           @RequestParam(required = false) List<ClaimStatus> status,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.list(actor, status, page, size);
    }

    @GetMapping("/{id}")
    public ClaimDetail get(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return service.get(actor, id);
    }

    @PostMapping(value = "/{id}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimDetail evidence(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                @RequestPart("file") MultipartFile file, @RequestParam EvidenceType evidenceType,
                                @RequestParam(required = false) String notes) {
        return service.addEvidence(actor, id, file, evidenceType, notes);
    }

    @GetMapping("/{id}/evidence/{evidenceId}/file")
    public ResponseEntity<Resource> evidenceFile(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                                 @PathVariable Long evidenceId,
                                                 @RequestParam(defaultValue = "true") boolean inline) {
        StoredFile f = service.evidenceFile(actor, id, evidenceId);
        return FileResponses.of(f, service.open(f), inline);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    @Operation(summary = "Ghi nhận định thẩm định; REQUEST_EVIDENCE → yêu cầu khách bổ sung")
    public ClaimDetail review(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                              @Valid @RequestBody ReviewClaimRequest req) {
        return service.review(actor, id, req);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    @Operation(summary = "Duyệt toàn phần/một phần (approvedAmount ≤ claimedAmount)")
    public ClaimDetail approve(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                               @Valid @RequestBody ApproveClaimRequest req) {
        return service.approve(actor, id, req);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public ClaimDetail reject(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                              @Valid @RequestBody RejectClaimRequest req) {
        return service.reject(actor, id, req.decisionNote());
    }

    @PostMapping("/{id}/settlement")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    @Operation(summary = "Ghi chi trả bồi thường (mô phỏng)")
    public ClaimDetail settle(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                              @Valid @RequestBody SettlementRequest req) {
        return service.settle(actor, id, req);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public ClaimDetail close(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                             @Valid @RequestBody(required = false) CloseClaimRequest req) {
        return service.close(actor, id, req == null ? null : req.note());
    }
}
