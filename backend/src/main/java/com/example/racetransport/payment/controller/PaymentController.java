package com.example.racetransport.payment.controller;

import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.payment.dto.PaymentDtos.CreateInvoiceRequest;
import com.example.racetransport.payment.dto.PaymentDtos.InvoiceResponse;
import com.example.racetransport.payment.dto.PaymentDtos.PaymentResponse;
import com.example.racetransport.payment.dto.PaymentDtos.SimulatePaymentRequest;
import com.example.racetransport.payment.entity.InvoiceStatus;
import com.example.racetransport.payment.service.InvoiceService;
import com.example.racetransport.payment.service.PaymentService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Invoices & Payments (simulation)")
@PreAuthorize("hasAnyRole('CUSTOMER','LOGISTICS_MANAGER')")
public class PaymentController {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    public PaymentController(InvoiceService invoiceService, PaymentService paymentService) {
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
    }

    @PostMapping("/api/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public InvoiceResponse createInvoice(@AuthenticationPrincipal UserPrincipal actor,
                                         @Valid @RequestBody CreateInvoiceRequest req) {
        return invoiceService.create(actor, req.quoteId());
    }

    @GetMapping("/api/invoices")
    public PageResponse<InvoiceResponse> invoices(@AuthenticationPrincipal UserPrincipal actor,
                                                  @RequestParam(required = false) List<InvoiceStatus> status,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return invoiceService.list(actor, status, page, size);
    }

    @GetMapping("/api/invoices/{id}")
    public InvoiceResponse invoice(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return invoiceService.get(actor, id);
    }

    @PostMapping("/api/payments/simulate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Thanh toán MÔ PHỎNG. Thẻ kết thúc 0002 = bị từ chối")
    public PaymentResponse simulate(@AuthenticationPrincipal UserPrincipal actor,
                                    @Valid @RequestBody SimulatePaymentRequest req) {
        return paymentService.simulate(actor, req);
    }

    @GetMapping("/api/payments")
    public PageResponse<PaymentResponse> payments(@AuthenticationPrincipal UserPrincipal actor,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return paymentService.list(actor, page, size);
    }

    @GetMapping("/api/payments/{id}")
    public PaymentResponse payment(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return paymentService.get(actor, id);
    }
}
