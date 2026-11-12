package com.example.racetransport.fleet.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Phương tiện vận chuyển.
 * Bảng: vehicle
 */
@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_code", nullable = false, unique = true, length = 20)
    private String vehicleCode;

    @Column(name = "plate_number", nullable = false, unique = true, length = 30)
    private String plateNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 40)
    private VehicleType vehicleType;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "capacity_horses", nullable = false)
    private Integer capacityHorses;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private VehicleStatus status;

    @Column(name = "current_location", length = 150)
    private String currentLocation;

    @Column(name = "climate_controlled", nullable = false)
    private Boolean climateControlled = Boolean.FALSE;

    @Column(name = "camera_monitoring", nullable = false)
    private Boolean cameraMonitoring = Boolean.FALSE;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "archived", nullable = false)
    private Boolean archived = Boolean.FALSE;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVehicleCode() { return vehicleCode; }
    public void setVehicleCode(String vehicleCode) { this.vehicleCode = vehicleCode; }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getCapacityHorses() { return capacityHorses; }
    public void setCapacityHorses(Integer capacityHorses) { this.capacityHorses = capacityHorses; }

    public VehicleStatus getStatus() { return status; }
    public void setStatus(VehicleStatus status) { this.status = status; }

    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }

    public Boolean getClimateControlled() { return climateControlled; }
    public void setClimateControlled(Boolean climateControlled) { this.climateControlled = climateControlled; }

    public Boolean getCameraMonitoring() { return cameraMonitoring; }
    public void setCameraMonitoring(Boolean cameraMonitoring) { this.cameraMonitoring = cameraMonitoring; }

    public LocalDate getLastMaintenanceDate() { return lastMaintenanceDate; }
    public void setLastMaintenanceDate(LocalDate lastMaintenanceDate) { this.lastMaintenanceDate = lastMaintenanceDate; }

    public LocalDate getNextMaintenanceDate() { return nextMaintenanceDate; }
    public void setNextMaintenanceDate(LocalDate nextMaintenanceDate) { this.nextMaintenanceDate = nextMaintenanceDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getArchived() { return archived; }
    public void setArchived(Boolean archived) { this.archived = archived; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
