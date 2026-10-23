package com.example.racetransport.claim.dto;

import com.example.racetransport.claim.entity.ClaimRecommendation;
import com.example.racetransport.claim.entity.ClaimStatus;
import com.example.racetransport.claim.entity.ClaimType;
import com.example.racetransport.claim.entity.EvidenceType;
import com.example.racetransport.claim.entity.SettlementStatus;
import com.example.racetransport.incident.entity.IncidentSeverity;
import com.example.racetransport.payment.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ClaimDtos {

    private ClaimDtos() { }

    public record CreateClaimRequest(
            @NotNull Long requestId,
            Long tripId,
            Long incidentId,
            Long horseId,
            @NotNull ClaimType claimType,
            @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal claimedAmount,
            @NotBlank @Size(max = 2000) String description) { }

    public record ReviewClaimRequest(
            @NotBlank @Size(max = 2000) String assessmentNote,
            @NotNull ClaimRecommendation recommendation,
            @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal recommendedAmount) { }

    /** Manager quyết định số tiền duyệt; KHÔNG tự động bằng giá trị khai báo của ngựa. */
    public record ApproveClaimRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal approvedAmount,
            @NotBlank @Size(max = 2000) String decisionNote) { }

    public record RejectClaimRequest(@NotBlank @Size(max = 2000) String decisionNote) { }

    public record SettlementRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal amount,
            PaymentMethod method,
            @Size(max = 500) String note) { }

    public record CloseClaimRequest(@Size(max = 1000) String note) { }

    public record EvidenceResponse(Long id, EvidenceType evidenceType, String notes, String fileName,
                                   String contentType, Long sizeBytes, String uploadedBy, LocalDateTime uploadedAt) { }

    public record AssessmentResponse(Long id, String assessmentNote, ClaimRecommendation recommendation,
                                     BigDecimal recommendedAmount, String assessedBy, LocalDateTime assessedAt) { }

    public record SettlementResponse(Long id, String settlementReference, BigDecimal amount, String currency,
                                     SettlementStatus status, LocalDateTime settledAt, String note, String settledBy) { }

    public record ClaimSummary(Long id, String claimNumber, Long requestId, String requestNumber, String customerName,
                               String horseName, String incidentNumber, ClaimType claimType, BigDecimal claimedAmount,
                               BigDecimal approvedAmount, String currency, ClaimStatus status,
                               LocalDateTime submittedAt, LocalDateTime decidedAt) { }

    public record ClaimDetail(Long id, String claimNumber, Long requestId, String requestNumber, Long tripId,
                              String tripNumber, Long incidentId, String incidentNumber, String incidentTitle,
                              IncidentSeverity incidentSeverity, Long horseId, String horseName,
                              BigDecimal declaredValueReference, Long customerId, String customerName,
                              ClaimType claimType, BigDecimal claimedAmount, BigDecimal approvedAmount,
                              BigDecimal settledAmount, String currency, String description, ClaimStatus status,
                              LocalDateTime submittedAt, String reviewedBy, LocalDateTime decidedAt,
                              String decisionNote, LocalDateTime closedAt, List<EvidenceResponse> evidence,
                              List<AssessmentResponse> assessments, List<SettlementResponse> settlements,
                              List<String> allowedActions) { }
}
