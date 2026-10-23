package com.example.racetransport.route.dto;

import com.example.racetransport.route.entity.RouteStatus;
import com.example.racetransport.route.entity.StopType;
import com.example.racetransport.transportationrequest.entity.TransportMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class RouteDtos {

    private RouteDtos() { }

    public record RouteStopRequest(
            @Min(1) Integer stopOrder,
            @NotNull StopType stopType,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 255) String address,
            @Size(min = 2, max = 2) String country,
            @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            @Min(0) Integer plannedArrivalOffsetMin,
            @Min(0) Integer plannedDurationMin,
            @Size(max = 1000) String instructions) { }

    public record RouteRequest(
            @Size(max = 20) @Pattern(regexp = "^[A-Z0-9\\-]*$", message = "Mã tuyến chỉ gồm A-Z, 0-9 và -") String routeCode,
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(min = 2, max = 2) String originCountry,
            @NotBlank @Size(max = 150) String originLocation,
            @NotBlank @Size(min = 2, max = 2) String destinationCountry,
            @NotBlank @Size(max = 150) String destinationLocation,
            @NotNull TransportMode transportMode,
            @DecimalMin("0.1") BigDecimal estimatedDistanceKm,
            @DecimalMin("0.1") BigDecimal estimatedDurationHours,
            RouteStatus status,
            Long alternativeOfId,
            @Size(max = 1000) String notes,
            @Size(max = 30) List<@Valid RouteStopRequest> stops) { }

    public record RouteStopResponse(Long id, Integer stopOrder, StopType stopType, String name, String address,
                                    String country, BigDecimal latitude, BigDecimal longitude,
                                    Integer plannedArrivalOffsetMin, Integer plannedDurationMin, String instructions) { }

    public record RouteSummary(Long id, String routeCode, String name, String originCountry, String originLocation,
                               String destinationCountry, String destinationLocation, TransportMode transportMode,
                               BigDecimal estimatedDistanceKm, BigDecimal estimatedDurationHours, RouteStatus status,
                               Long alternativeOfId, int stopCount, LocalDateTime updatedAt) { }

    public record RouteDetail(Long id, String routeCode, String name, String originCountry, String originLocation,
                              String destinationCountry, String destinationLocation, TransportMode transportMode,
                              BigDecimal estimatedDistanceKm, BigDecimal estimatedDurationHours, RouteStatus status,
                              Long alternativeOfId, String alternativeOfName, String notes,
                              List<RouteStopResponse> stops, List<RouteSummary> alternatives, long tripCount,
                              LocalDateTime updatedAt) { }
}
