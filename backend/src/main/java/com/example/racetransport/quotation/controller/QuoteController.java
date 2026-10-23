package com.example.racetransport.quotation.controller;

import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.quotation.dto.QuoteDtos.CreateQuoteRequest;
import com.example.racetransport.quotation.dto.QuoteDtos.PricingRateResponse;
import com.example.racetransport.quotation.dto.QuoteDtos.QuoteResponse;
import com.example.racetransport.quotation.dto.QuoteDtos.QuoteUpdateRequest;
import com.example.racetransport.quotation.dto.QuoteDtos.RespondRequest;
import com.example.racetransport.quotation.entity.QuoteStatus;
import com.example.racetransport.quotation.service.QuoteService;
import com.example.racetransport.security.UserPrincipal;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Quotes")
public class QuoteController {

    private final QuoteService service;

    public QuoteController(QuoteService service) {
        this.service = service;
    }

    @PostMapping("/api/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    @Operation(summary = "Tạo bản nháp Final Quote (sao chép từ ước tính hoặc phiên bản trước)")
    public QuoteResponse create(@AuthenticationPrincipal UserPrincipal actor,
                                @Valid @RequestBody CreateQuoteRequest req) {
        return service.createFinal(actor, req.requestId());
    }

    @GetMapping("/api/quotes")
    @PreAuthorize("hasAnyRole('CUSTOMER','LOGISTICS_MANAGER')")
    public PageResponse<QuoteResponse> list(@AuthenticationPrincipal UserPrincipal actor,
                                            @RequestParam(required = false) List<QuoteStatus> status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.listFinal(actor, status, page, size);
    }

    @GetMapping("/api/quotes/{id}")
    public QuoteResponse get(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return service.get(actor, id);
    }

    @PutMapping("/api/quotes/{id}")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    @Operation(summary = "Sửa dòng chi phí (chỉ bản nháp). Tổng do backend tính")
    public QuoteResponse update(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                @Valid @RequestBody QuoteUpdateRequest req) {
        return service.update(actor, id, req);
    }

    @PostMapping("/api/quotes/{id}/send")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public QuoteResponse send(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return service.send(actor, id);
    }

    @PostMapping("/api/quotes/{id}/accept")
    @PreAuthorize("hasRole('CUSTOMER')")
    public QuoteResponse accept(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                @Valid @RequestBody(required = false) RespondRequest body) {
        return service.accept(actor, id, body);
    }

    @PostMapping("/api/quotes/{id}/reject")
    @PreAuthorize("hasRole('CUSTOMER')")
    public QuoteResponse reject(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                @Valid @RequestBody RespondRequest body) {
        return service.reject(actor, id, body);
    }

    @GetMapping("/api/transportation-requests/{requestId}/quotes")
    public List<QuoteResponse> forRequest(@AuthenticationPrincipal UserPrincipal actor,
                                          @PathVariable Long requestId) {
        return service.listForRequest(actor, requestId);
    }

    @GetMapping("/api/pricing-rates")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public List<PricingRateResponse> pricingRates() {
        return service.pricingRates();
    }
}
