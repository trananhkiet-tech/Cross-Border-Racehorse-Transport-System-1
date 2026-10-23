package com.example.racetransport.transportationrequest.controller;

import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.transportationrequest.dto.RequestDtos.DecisionRequest;
import com.example.racetransport.transportationrequest.dto.RequestDtos.RequestDetail;
import com.example.racetransport.transportationrequest.dto.RequestDtos.RequestSummary;
import com.example.racetransport.transportationrequest.dto.RequestDtos.RequestUpsert;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.service.RequestService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transportation-requests")
@Tag(name = "Transportation Requests")
public class TransportationRequestController {

    private final RequestService service;

    public TransportationRequestController(RequestService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','LOGISTICS_MANAGER','TRANSPORT_SPECIALIST','FLEET_ROUTE_COORDINATOR')")
    @Operation(summary = "Danh sách yêu cầu (Customer chỉ thấy của mình)")
    public PageResponse<RequestSummary> list(@AuthenticationPrincipal UserPrincipal actor,
                                             @RequestParam(required = false) List<RequestStatus> status,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.list(actor, status, q, page, size);
    }

    @GetMapping("/{id}")
    public RequestDetail get(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return service.get(actor, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Tạo yêu cầu ở trạng thái DRAFT")
    public RequestDetail create(@AuthenticationPrincipal UserPrincipal actor, @Valid @RequestBody RequestUpsert req) {
        return service.create(actor, req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public RequestDetail update(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                @Valid @RequestBody RequestUpsert req) {
        return service.update(actor, id, req);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Gửi yêu cầu: sinh checklist tuân thủ và chi phí ước tính")
    public RequestDetail submit(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return service.submit(actor, id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER','LOGISTICS_MANAGER')")
    public RequestDetail cancel(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                @Valid @RequestBody(required = false) DecisionRequest body) {
        return service.cancel(actor, id, body);
    }

    @PostMapping("/{id}/start-review")
    @PreAuthorize("hasRole('TRANSPORT_SPECIALIST')")
    public RequestDetail startReview(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return service.startDocumentReview(actor, id);
    }

    @PostMapping("/{id}/complete-document-review")
    @PreAuthorize("hasRole('TRANSPORT_SPECIALIST')")
    public RequestDetail completeDocumentReview(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                                @Valid @RequestBody(required = false) DecisionRequest body) {
        return service.completeDocumentReview(actor, id, body);
    }

    @PostMapping("/{id}/complete-compliance")
    @PreAuthorize("hasRole('TRANSPORT_SPECIALIST')")
    @Operation(summary = "Xác nhận hồ sơ tuân thủ đầy đủ → chờ Manager phê duyệt")
    public RequestDetail completeCompliance(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                            @Valid @RequestBody(required = false) DecisionRequest body) {
        return service.completeCompliance(actor, id, body);
    }

    @PostMapping("/{id}/request-information")
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','TRANSPORT_SPECIALIST')")
    public RequestDetail requestInformation(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                            @Valid @RequestBody DecisionRequest body) {
        return service.requestInformation(actor, id, body);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public RequestDetail approve(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                 @Valid @RequestBody(required = false) DecisionRequest body) {
        return service.approve(actor, id, body);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public RequestDetail reject(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                @Valid @RequestBody DecisionRequest body) {
        return service.reject(actor, id, body);
    }
}
