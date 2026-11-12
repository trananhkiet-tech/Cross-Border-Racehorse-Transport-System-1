package com.example.racetransport.trip.dto;

import com.example.racetransport.fleet.entity.VehicleType;
import com.example.racetransport.handover.entity.HandoverStatus;
import com.example.racetransport.health.entity.HorseCondition;
import com.example.racetransport.incident.entity.IncidentSeverity;
import com.example.racetransport.incident.entity.IncidentStatus;
import com.example.racetransport.route.dto.RouteDtos.RouteStopResponse;
import com.example.racetransport.tracking.entity.TrackingEventType;
import com.example.racetransport.transportationrequest.entity.TransportMode;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.user.entity.StaffRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class TripDtos {

    private TripDtos() { }

    // ------------------------------------------------------------- requests
    public record StaffAssignmentInput(@NotNull Long staffId, @NotNull StaffRole role) { }

    public record CreateTripRequest(
            @NotNull Long requestId,
            @NotNull Long routeId,
            @NotNull LocalDateTime plannedDeparture,
            @NotNull LocalDateTime plannedArrival,
            Long vehicleId,
            @Size(max = 2000) String transportInstructions,
            @Size(max = 1000) String notes,
            @Size(max = 6) List<@Valid StaffAssignmentInput> staff) { }

    public record UpdateTripPlanRequest(
            @NotNull LocalDateTime plannedDeparture,
            @NotNull LocalDateTime plannedArrival,
            @Size(max = 2000) String transportInstructions,
            @Size(max = 1000) String notes) { }

    public record AssignVehicleRequest(@NotNull Long vehicleId) { }

    public record AssignStaffRequest(@NotNull Long staffId, @NotNull StaffRole role) { }

    public record StatusUpdateRequest(
            @NotNull TripStatus status,
            @Size(max = 1000) String note,
            @Size(max = 150) String location,
            @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            @Min(0) @Max(10080) Integer delayMinutes) { }

    public record CheckpointRequest(
            Long routeStopId,
            @NotNull TrackingEventType eventType,
            @Size(max = 150) String location,
            @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            @Min(0) @Max(10080) Integer delayMinutes,
            @Size(max = 1000) String note,
            LocalDateTime eventTime,
            TripStatus status) { }

    public record CancelTripRequest(@Size(max = 1000) String note) { }

    // ------------------------------------------------------------- responses
    public record StaffResponse(Long assignmentId, Long staffId, String fullName, String phone, StaffRole role,
                                LocalDateTime assignedAt) { }

    public record TripHorseResponse(Long horseId, String name, String microchipNumber, String breed,
                                    String stallTypeName, Integer stallWidthCm, String specialRequirements,
                                    boolean preCheckDone, Boolean fitForTravelObserved, boolean arrivalCheckDone,
                                    HorseCondition arrivalCondition, Long handoverId, HandoverStatus handoverStatus) { }

    public record VehicleRef(Long id, String vehicleCode, String plateNumber, VehicleType vehicleType,
                             Integer capacityHorses, boolean climateControlled) { }

    public record IncidentRef(Long id, String incidentNumber, String title, IncidentSeverity severity,
                              IncidentStatus status, LocalDateTime reportedAt) { }

    public record TripSummary(Long id, String tripNumber, Long requestId, String requestNumber, String customerName,
                              Long routeId, String routeName, String originLocation, String destinationLocation,
                              TransportMode transportMode, String vehicleCode, LocalDateTime plannedDeparture,
                              LocalDateTime plannedArrival, LocalDateTime actualDeparture,
                              LocalDateTime actualArrival, TripStatus status, String currentLocation,
                              boolean operationBlocked, List<String> staffNames) { }

    public record TripDetail(Long id, String tripNumber, Long requestId, String requestNumber, Long customerId,
                             String customerName, String customerPhone, Long routeId, String routeCode,
                             String routeName, TransportMode transportMode, String originLocation,
                             String destinationLocation, String destinationAddress,
                             LocalDateTime plannedDeparture, LocalDateTime plannedArrival,
                             LocalDateTime actualDeparture, LocalDateTime actualArrival, TripStatus status,
                             String currentLocation, Long currentStopId, boolean operationBlocked,
                             String transportInstructions, String notes, String coordinatorName,
                             VehicleRef vehicle, List<StaffResponse> staff, List<TripHorseResponse> horses,
                             List<RouteStopResponse> stops, List<IncidentRef> incidents, long totalDelayMinutes,
                             List<TripStatus> nextStatuses, List<String> allowedActions,
                             LocalDateTime createdAt) { }

    public record StopProgress(RouteStopResponse stop, LocalDateTime plannedArrival, LocalDateTime actualArrival,
                               LocalDateTime actualDeparture, String state) { }

    public record TrackingEventResponse(Long id, TrackingEventType eventType, LocalDateTime eventTime,
                                        String location, BigDecimal latitude, BigDecimal longitude,
                                        TripStatus tripStatus, Integer delayMinutes, String note,
                                        String recordedBy, Long routeStopId, String routeStopName) { }

    public record TrackingResponse(Long tripId, String tripNumber, TripStatus status, String currentLocation,
                                   Long currentStopId, int progressPercent, LocalDateTime plannedArrival,
                                   LocalDateTime estimatedArrival, long totalDelayMinutes,
                                   LocalDateTime lastUpdate, boolean operationBlocked, List<StopProgress> stops,
                                   List<TrackingEventResponse> events) { }

    public record TimelineItem(LocalDateTime time, String kind, String title, String description, String status,
                               String tone, String actor, Long refId) { }
}
