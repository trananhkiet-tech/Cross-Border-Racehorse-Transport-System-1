package com.example.racetransport.payment.dto;

import com.example.racetransport.payment.entity.InvoiceStatus;
import com.example.racetransport.payment.entity.PaymentMethod;
import com.example.racetransport.payment.entity.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PaymentDtos {

    private PaymentDtos() { }

    public record PaymentResponse(Long id, String paymentReference, Long invoiceId, String invoiceNumber,
                                  String requestNumber, String customerName, BigDecimal amount, String currency,
                                  PaymentMethod method, PaymentStatus status, String cardLast4,
                                  LocalDateTime paidAt, String note, LocalDateTime createdAt) { }

    public record InvoiceResponse(Long id, String invoiceNumber, Long quoteId, String quoteNumber, Long requestId,
                                  String requestNumber, Long customerId, String customerName, LocalDate issueDate,
                                  LocalDate dueDate, String currency, BigDecimal totalAmount, BigDecimal paidAmount,
                                  BigDecimal outstandingAmount, BigDecimal depositPercent, BigDecimal depositAmount,
                                  BigDecimal minimumNextPayment, InvoiceStatus status, boolean overdue,
                                  List<PaymentResponse> payments) { }

    public record CreateInvoiceRequest(@NotNull Long quoteId) { }

    /**
     * Thanh toán MÔ PHỎNG: không nhận số thẻ thật. Chỉ nhận 4 số cuối để mô phỏng
     * (kết thúc "0002" = giao dịch bị từ chối). Backend tự kiểm tra số tiền hợp lệ.
     */
    public record SimulatePaymentRequest(
            @NotNull Long invoiceId,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 16, fraction = 2) BigDecimal amount,
            @NotNull PaymentMethod method,
            @Pattern(regexp = "^\\d{4}$", message = "Chỉ nhập 4 số cuối (mô phỏng)") String cardLast4,
            @Size(max = 500) String note) { }
}
