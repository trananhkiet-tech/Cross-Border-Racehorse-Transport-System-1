package com.example.racetransport.transportationrequest.dto;

import com.example.racetransport.quotation.entity.QuoteStatus;
import com.example.racetransport.transportationrequest.entity.MovementType;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportMode;
import com.example.racetransport.transportationrequest.entity.TransportPurpose;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.user.entity.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RequestDtos {

    private RequestDtos() { }

    /** Ngựa trong yêu cầu: chọn loại chuồng và khai báo giá trị (tham chiếu bảo hiểm/hải quan). */
    public record RequestHorseInput(
            @NotNull Long horseId,
            @NotNull Long stallTypeId,
            @NotNull @DecimalMin(value = "0.01", message = "Giá trị khai báo phải lớn hơn 0")
            @DecimalMax(value = "100000000") @Digits(integer = 16, fraction = 2) BigDecimal declaredValue,
            @Size(max = 500) String notes) { }

    public record RequestUpsert(
            @NotBlank @Size(min = 2, max = 2) String originCountry,
            @NotBlank @Size(max = 255) String originAddress,
            @NotBlank @Size(min = 2, max = 2) String destinationCountry,
            @NotBlank @Size(max = 255) String destinationAddress,
            @NotNull LocalDate requestedDepartureDate,
            LocalDate returnDate,
            @NotNull MovementType movementType,
            @NotNull TransportPurpose purpose,
            @NotNull TransportMode transportMode,
            @Pattern(regexp = "USD|VND|EUR|SGD|THB", message = "Tiền tệ không được hỗ trợ") String currency,
            @Size(max = 1000) String specialRequirements,
            @Size(max = 1000) String notes,
            @NotEmpty(message = "Cần chọn ít nhất một ngựa") @Size(max = 12) List<@Valid RequestHorseInput> horses) { }

    /** Ghi chú quyết định. Bắt buộc với reject / request-information (kiểm tra ở service). */
    public record DecisionRequest(@Size(max = 1000) String note) { }

    public record RequestHorseResponse(Long id, Long horseId, String horseName, String microchipNumber,
                                       String passportNumber, String breed, Long stallTypeId, String stallTypeCode,
                                       String stallTypeName, Integer stallWidthCm, BigDecimal stallSurcharge,
                                       BigDecimal declaredValue, String notes) { }

    public record StatusHistoryResponse(RequestStatus fromStatus, RequestStatus toStatus, String changedBy,
                                        Role changedByRole, String note, LocalDateTime changedAt) { }

    public record ComplianceSummary(long total, long approved, long pending, long missing, long problems,
                                    boolean complete) { }

    public record QuoteRef(Long id, String quoteNumber, Integer versionNo, QuoteStatus status,
                           BigDecimal totalAmount, String currency) { }

    public record TripRef(Long id, String tripNumber, TripStatus status, LocalDateTime plannedDeparture) { }

    public record RequestSummary(Long id, String requestNumber, Long customerId, String customerName,
                                 String originCountry, String originAddress, String destinationCountry,
                                 String destinationAddress, LocalDate requestedDepartureDate,
                                 MovementType movementType, TransportPurpose purpose, TransportMode transportMode,
                                 RequestStatus status, int horseCount, List<String> horseNames,
                                 BigDecimal declaredValueTotal, String currency,
                                 LocalDateTime createdAt, LocalDateTime submittedAt) { }

    public record RequestDetail(Long id, String requestNumber, Long customerId, String customerName,
                                String customerEmail, String customerPhone,
                                String originCountry, String originAddress, String destinationCountry,
                                String destinationAddress, LocalDate requestedDepartureDate, LocalDate returnDate,
                                MovementType movementType, TransportPurpose purpose, TransportMode transportMode,
                                BigDecimal declaredValueTotal, String currency, String specialRequirements,
                                String notes, RequestStatus status, String lastDecisionNote,
                                LocalDateTime createdAt, LocalDateTime submittedAt, LocalDateTime approvedAt,
                                LocalDateTime completedAt,
                                List<RequestHorseResponse> horses,
                                List<StatusHistoryResponse> history,
                                ComplianceSummary compliance,
                                QuoteRef estimate,
                                QuoteRef latestQuote,
                                Long invoiceId,
                                List<TripRef> trips,
                                List<String> allowedActions) { }
}
