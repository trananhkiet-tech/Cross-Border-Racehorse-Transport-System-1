package com.example.racetransport.health.dto;

import com.example.racetransport.health.entity.HorseBehavior;
import com.example.racetransport.health.entity.HorseCondition;
import com.example.racetransport.health.entity.IntakeLevel;
import com.example.racetransport.health.entity.MovementCondition;
import com.example.racetransport.health.entity.ObservationSeverity;
import com.example.racetransport.health.entity.StressLevel;
import com.example.racetransport.incident.entity.OperationalDecision;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO sức khỏe. Lưu ý nghiệp vụ: Driver/Escort chỉ QUAN SÁT và BÁO CÁO, không chẩn đoán;
 * tư vấn thú y là bản ghi từ bác sĩ bên ngoài.
 */
public final class HealthDtos {

    private HealthDtos() { }

    public record PreTransportCheckRequest(
            @NotNull Long tripId,
            @NotNull Long horseId,
            @DecimalMin("34.0") @DecimalMax("43.0") BigDecimal temperatureC,
            @NotNull HorseCondition generalCondition,
            Boolean identityVerified,
            Boolean documentsPresent,
            Boolean loadingOk,
            @NotNull Boolean fitForTravelObserved,
            @Size(max = 1000) String notes) { }

    public record PreTransportCheckResponse(Long id, Long tripId, Long horseId, String horseName,
                                            BigDecimal temperatureC, HorseCondition generalCondition,
                                            boolean identityVerified, boolean documentsPresent, boolean loadingOk,
                                            boolean fitForTravelObserved, String notes, String checkedBy,
                                            LocalDateTime checkedAt) { }

    public record ObservationRequest(
            @NotNull Long tripId,
            @NotNull Long horseId,
            LocalDateTime observedAt,
            @Size(max = 150) String location,
            @NotNull IntakeLevel appetite,
            @NotNull IntakeLevel waterIntake,
            @NotNull HorseBehavior behavior,
            @NotNull StressLevel stressLevel,
            @NotNull MovementCondition movement,
            Boolean visibleInjury,
            @Size(max = 500) String injuryDescription,
            @DecimalMin("34.0") @DecimalMax("43.0") BigDecimal temperatureC,
            ObservationSeverity severity,
            @Size(max = 1000) String notes,
            /** Driver chọn báo Incident ngay từ quan sát nghiêm trọng. */
            Boolean reportIncident) { }

    public record AttachmentRef(Long id, String fileName, String contentType) { }

    public record ObservationResponse(Long id, Long tripId, String tripNumber, Long horseId, String horseName,
                                      String author, LocalDateTime observedAt, String location, IntakeLevel appetite,
                                      IntakeLevel waterIntake, HorseBehavior behavior, StressLevel stressLevel,
                                      MovementCondition movement, boolean visibleInjury, String injuryDescription,
                                      BigDecimal temperatureC, ObservationSeverity severity, String notes,
                                      Long incidentId, List<AttachmentRef> photos) { }

    public record ArrivalCheckRequest(
            @NotNull Long tripId,
            @NotNull Long horseId,
            @NotNull HorseCondition condition,
            Boolean visibleInjury,
            @Size(max = 1000) String notes) { }

    public record ArrivalCheckResponse(Long id, Long tripId, Long horseId, String horseName, HorseCondition condition,
                                       boolean visibleInjury, String notes, String checkedBy,
                                       LocalDateTime checkedAt) { }

    public record VetConsultationRequest(
            Long incidentId,
            Long tripId,
            Long horseId,
            @NotBlank @Size(max = 150) String veterinarianName,
            @Size(max = 150) String clinic,
            @Size(max = 150) String contact,
            LocalDateTime consultedAt,
            @NotBlank @Size(max = 2000) String advice,
            OperationalDecision recommendedAction) { }

    public record VetConsultationResponse(Long id, Long incidentId, String incidentNumber, Long tripId,
                                          String tripNumber, Long horseId, String horseName, String veterinarianName,
                                          String clinic, String contact, LocalDateTime consultedAt, String advice,
                                          OperationalDecision recommendedAction, Long reportFileId,
                                          String reportFileName, String recordedBy, LocalDateTime createdAt) { }

    /** Lịch sử sức khỏe của một ngựa qua các chuyến. */
    public record HorseHealthHistory(Long horseId, String horseName, List<PreTransportCheckResponse> preTransportChecks,
                                     List<ObservationResponse> observations, List<ArrivalCheckResponse> arrivalChecks,
                                     List<VetConsultationResponse> consultations, String disclaimer) { }
}
