package com.example.racetransport.quotation.dto;

import com.example.racetransport.quotation.entity.CostCategory;
import com.example.racetransport.quotation.entity.PricingUnit;
import com.example.racetransport.quotation.entity.QuoteStatus;
import com.example.racetransport.quotation.entity.QuoteType;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class QuoteDtos {

    private QuoteDtos() { }

    public record QuoteItemResponse(Long id, CostCategory category, String description, BigDecimal quantity,
                                    BigDecimal unitPrice, BigDecimal amount, BigDecimal estimatedAmount,
                                    boolean confirmed, String source, Integer sortOrder) { }

    public record QuoteResponse(Long id, String quoteNumber, Long requestId, String requestNumber,
                                RequestStatus requestStatus, Long customerId, String customerName, String route,
                                Integer versionNo, QuoteType quoteType, QuoteStatus status, String currency,
                                BigDecimal subtotal, BigDecimal discountTotal, BigDecimal totalAmount,
                                BigDecimal estimatedTotal, BigDecimal declaredValueTotal, LocalDate validUntil,
                                boolean expired, String notes, String createdBy, LocalDateTime createdAt,
                                LocalDateTime updatedAt, LocalDateTime sentAt, LocalDateTime acceptedAt,
                                LocalDateTime rejectedAt, String customerResponseNote, Long invoiceId,
                                List<QuoteItemResponse> items, List<String> allowedActions) { }

    /** Dòng chi phí do Manager nhập; amount KHÔNG nhận từ client mà backend tự tính. */
    public record QuoteItemInput(
            @NotNull CostCategory category,
            @NotBlank @Size(max = 255) String description,
            @NotNull @DecimalMin(value = "0.01") @DecimalMax("100000") @Digits(integer = 10, fraction = 2) BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.00") @DecimalMax("100000000") @Digits(integer = 16, fraction = 2) BigDecimal unitPrice,
            Boolean confirmed,
            @Size(max = 150) String source,
            BigDecimal estimatedAmount) { }

    public record QuoteUpdateRequest(
            LocalDate validUntil,
            @Size(max = 2000) String notes,
            @NotEmpty(message = "Báo giá cần ít nhất một dòng chi phí") @Size(max = 60) List<@Valid QuoteItemInput> items) { }

    public record CreateQuoteRequest(@NotNull Long requestId) { }

    public record RespondRequest(@Size(max = 1000) String note) { }

    public record PricingRateResponse(Long id, String code, CostCategory category, String description,
                                      PricingUnit unit, BigDecimal unitPrice, String currency,
                                      TransportMode transportMode, String destinationCountry, boolean active) { }
}
