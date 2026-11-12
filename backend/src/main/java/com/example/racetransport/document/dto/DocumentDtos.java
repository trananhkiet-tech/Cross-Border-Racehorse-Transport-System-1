package com.example.racetransport.document.dto;

import com.example.racetransport.document.entity.AttachmentRefType;
import com.example.racetransport.document.entity.DocumentReviewStatus;
import com.example.racetransport.document.entity.DocumentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DocumentDtos {

    private DocumentDtos() { }

    public record DocumentResponse(Long id, DocumentType documentType, String title, String documentNumber,
                                   String issuingCountry, LocalDate issueDate, LocalDate expiryDate,
                                   DocumentReviewStatus reviewStatus, String reviewNote, String reviewerName,
                                   LocalDateTime reviewedAt, Long requestId, String requestNumber, Long horseId,
                                   String horseName, String customerName, String fileName, String contentType,
                                   Long sizeBytes, String uploadedByName, LocalDateTime uploadedAt,
                                   Long daysToExpiry, boolean expired) { }

    /** Specialist rà soát: APPROVED / REJECTED / NEEDS_UPDATE (+ ghi chú). */
    public record ReviewRequest(@NotNull DocumentReviewStatus status, @Size(max = 1000) String note) { }

    public record AttachmentResponse(Long id, AttachmentRefType refType, Long refId, String caption,
                                     String fileName, String contentType, Long sizeBytes, LocalDateTime createdAt) { }
}
