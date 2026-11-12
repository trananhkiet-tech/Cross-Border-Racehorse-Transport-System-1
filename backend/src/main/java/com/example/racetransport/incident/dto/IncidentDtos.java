package com.example.racetransport.incident.dto;

import com.example.racetransport.health.dto.HealthDtos.AttachmentRef;
import com.example.racetransport.health.dto.HealthDtos.VetConsultationResponse;
import com.example.racetransport.incident.entity.IncidentActionType;
import com.example.racetransport.incident.entity.IncidentSeverity;
import com.example.racetransport.incident.entity.IncidentStatus;
import com.example.racetransport.incident.entity.IncidentType;
import com.example.racetransport.incident.entity.OperationalDecision;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.user.entity.Role;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class IncidentDtos {

    private IncidentDtos() { }

    public record ReportIncidentRequest(
            @NotNull Long tripId,
            Long horseId,
            @NotNull IncidentType incidentType,
            @NotNull IncidentSeverity severity,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 2000) String description,
            @Size(max = 150) String location,
            LocalDateTime occurredAt) { }

    public record IncidentActionRequest(
            @NotNull IncidentActionType actionType,
            @NotBlank @Size(max = 2000) String note,
            OperationalDecision decision,
            Long newRouteId,
            @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal extraCost,
            @Pattern(regexp = "USD|VND|EUR|SGD|THB") String currency,
            Long consultationId) { }

    public record IncidentStatusRequest(@NotNull IncidentStatus status, @Size(max = 2000) String note) { }

    public record IncidentActionResponse(Long id, IncidentActionType actionType, OperationalDecision decision,
                                         String note, String performedBy, Role performedByRole,
                                         LocalDateTime performedAt, Long consultationId, Long newRouteId,
                                         String newRouteName, BigDecimal extraCost, String currency) { }

    public record IncidentSummary(Long id, String incidentNumber, Long tripId, String tripNumber, Long requestId,
                                  String requestNumber, String customerName, Long horseId, String horseName,
                                  IncidentType incidentType, IncidentSeverity severity, String title,
                                  IncidentStatus status, OperationalDecision decision, String location,
                                  LocalDateTime reportedAt, String reportedBy, boolean tripBlocked) { }

    public record RouteOption(Long id, String routeCode, String name) { }

    public record IncidentDetail(Long id, String incidentNumber, Long tripId, String tripNumber, TripStatus tripStatus,
                                 String tripLocation, String routeName, Long routeId, Long requestId,
                                 String requestNumber, String customerName, Long horseId, String horseName,
                                 IncidentType incidentType, IncidentSeverity severity, String title,
                                 String description, String location, IncidentStatus status,
                                 LocalDateTime reportedAt, String reportedBy, OperationalDecision decision,
                                 String decidedBy, LocalDateTime decidedAt, LocalDateTime resolvedAt,
                                 String resolutionNote, boolean tripBlocked, List<IncidentActionResponse> actions,
                                 List<VetConsultationResponse> consultations, List<AttachmentRef> attachments,
                                 List<RouteOption> rerouteOptions, List<String> allowedActions) { }
}
