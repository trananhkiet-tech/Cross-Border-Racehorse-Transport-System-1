package com.example.racetransport.compliance.dto;

import com.example.racetransport.compliance.entity.ChecklistItemStatus;
import com.example.racetransport.compliance.entity.ComplianceCategory;
import com.example.racetransport.document.entity.DocumentReviewStatus;
import com.example.racetransport.document.entity.DocumentType;
import com.example.racetransport.transportationrequest.dto.RequestDtos.ComplianceSummary;
import com.example.racetransport.transportationrequest.entity.MovementType;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportPurpose;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ComplianceDtos {

    private ComplianceDtos() { }

    public static final String DISCLAIMER = "Checklist được sinh từ danh mục yêu cầu DEMO theo tuyến, loại di chuyển "
            + "và mục đích; không phải hướng dẫn pháp lý chính thức của bất kỳ quốc gia nào.";

    public record RequirementResponse(Long id, String code, String originCountry, String destinationCountry,
                                      MovementType movementType, TransportPurpose purpose,
                                      ComplianceCategory category, DocumentType documentType, String title,
                                      String description, Integer validityDays, boolean perHorse, boolean mandatory,
                                      LocalDate effectiveFrom, LocalDate effectiveTo, boolean active) { }

    public record RequirementRequest(
            @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Z0-9_\\-]+$", message = "Mã chỉ gồm A-Z, 0-9, _ -")
            String code,
            @Size(min = 2, max = 2) String originCountry,
            @Size(min = 2, max = 2) String destinationCountry,
            MovementType movementType,
            TransportPurpose purpose,
            @NotNull ComplianceCategory category,
            @NotNull DocumentType documentType,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 1000) String description,
            @Min(1) Integer validityDays,
            Boolean perHorse,
            Boolean mandatory,
            @NotNull LocalDate effectiveFrom,
            LocalDate effectiveTo,
            Boolean active) { }

    public record ChecklistItemResponse(Long id, Long requirementId, String requirementCode, String title,
                                        String description, ComplianceCategory category, DocumentType documentType,
                                        boolean mandatory, Integer validityDays, Long horseId, String horseName,
                                        ChecklistItemStatus status, Long documentId, String documentFileName,
                                        DocumentReviewStatus documentStatus, LocalDate documentExpiryDate,
                                        String reviewerNote, String checkedBy, LocalDateTime checkedAt,
                                        LocalDate dueDate) { }

    public record ChecklistResponse(Long requestId, String requestNumber, RequestStatus requestStatus,
                                    String route, LocalDate requestedDepartureDate, ComplianceSummary summary,
                                    List<ChecklistItemResponse> items, String disclaimer) { }

    /** Specialist cập nhật một mục checklist. */
    public record PatchChecklistItemRequest(@NotNull ChecklistItemStatus status, @Size(max = 1000) String note,
                                            Long documentId) { }

    public record OpenItemResponse(Long itemId, Long requestId, String requestNumber, String customerName,
                                   LocalDate requestedDepartureDate, String title, DocumentType documentType,
                                   ComplianceCategory category, String horseName, ChecklistItemStatus status,
                                   String reviewerNote, LocalDate dueDate) { }
}
