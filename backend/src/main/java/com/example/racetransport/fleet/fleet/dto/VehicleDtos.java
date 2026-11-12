package com.example.racetransport.fleet.dto;

import com.example.racetransport.fleet.entity.VehicleStatus;
import com.example.racetransport.fleet.entity.VehicleType;
import com.example.racetransport.trip.entity.TripStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class VehicleDtos {

    private VehicleDtos() { }

    public record VehicleRequest(
            @NotBlank @Size(max = 20) String vehicleCode,
            @NotBlank @Size(max = 30) String plateNumber,
            @NotNull VehicleType vehicleType,
            @Size(max = 100) String model,
            @NotNull @Min(1) @Max(12) Integer capacityHorses,
            @Size(max = 150) String currentLocation,
            Boolean climateControlled,
            Boolean cameraMonitoring,
            LocalDate lastMaintenanceDate,
            LocalDate nextMaintenanceDate,
            @Size(max = 1000) String notes) { }

    /** Coordinator đổi trạng thái vận hành (không đặt IN_USE thủ công). */
    public record VehicleStatusRequest(@NotNull VehicleStatus status, @Size(max = 500) String note) { }

    public record VehicleTripRef(Long tripId, String tripNumber, TripStatus status, LocalDateTime plannedDeparture,
                                 LocalDateTime plannedArrival, String routeName) { }

    public record VehicleResponse(Long id, String vehicleCode, String plateNumber, VehicleType vehicleType,
                                  String model, Integer capacityHorses, VehicleStatus status, String currentLocation,
                                  boolean climateControlled, boolean cameraMonitoring, LocalDate lastMaintenanceDate,
                                  LocalDate nextMaintenanceDate, boolean maintenanceDue, String notes,
                                  boolean archived, LocalDateTime updatedAt, List<VehicleTripRef> upcomingTrips) { }
}
