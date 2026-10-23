package com.example.racetransport.trip.entity;

import com.example.racetransport.fleet.entity.Vehicle;
import com.example.racetransport.route.entity.Route;
import com.example.racetransport.route.entity.RouteStop;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.user.entity.AppUser;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Chuyến vận chuyển. Một yêu cầu có thể có nhiều chuyến (nhiều chặng).
 * Bảng: transportation_trip
 */
@Entity
@Table(name = "transportation_trip")
public class TransportationTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_number", nullable = false, unique = true, length = 30)
    private String tripNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private TransportationRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coordinator_id", nullable = false)
    private AppUser coordinator;

    @Column(name = "planned_departure", nullable = false)
    private LocalDateTime plannedDeparture;

    @Column(name = "planned_arrival", nullable = false)
    private LocalDateTime plannedArrival;

    @Column(name = "actual_departure")
    private LocalDateTime actualDeparture;

    @Column(name = "actual_arrival")
    private LocalDateTime actualArrival;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private TripStatus status;

    @Column(name = "current_location", length = 150)
    private String currentLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_stop_id")
    private RouteStop currentStop;

    /** Bị chặn do sự cố khẩn cấp chờ quyết định Manager */
    @Column(name = "operation_blocked", nullable = false)
    private Boolean operationBlocked = Boolean.FALSE;

    @Column(name = "transport_instructions", length = 2000)
    private String transportInstructions;

    @Column(name = "notes", length = 1000)
    private String notes;

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

    public String getTripNumber() { return tripNumber; }
    public void setTripNumber(String tripNumber) { this.tripNumber = tripNumber; }

    public TransportationRequest getRequest() { return request; }
    public void setRequest(TransportationRequest request) { this.request = request; }

    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public AppUser getCoordinator() { return coordinator; }
    public void setCoordinator(AppUser coordinator) { this.coordinator = coordinator; }

    public LocalDateTime getPlannedDeparture() { return plannedDeparture; }
    public void setPlannedDeparture(LocalDateTime plannedDeparture) { this.plannedDeparture = plannedDeparture; }

    public LocalDateTime getPlannedArrival() { return plannedArrival; }
    public void setPlannedArrival(LocalDateTime plannedArrival) { this.plannedArrival = plannedArrival; }

    public LocalDateTime getActualDeparture() { return actualDeparture; }
    public void setActualDeparture(LocalDateTime actualDeparture) { this.actualDeparture = actualDeparture; }

    public LocalDateTime getActualArrival() { return actualArrival; }
    public void setActualArrival(LocalDateTime actualArrival) { this.actualArrival = actualArrival; }

    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }

    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }

    public RouteStop getCurrentStop() { return currentStop; }
    public void setCurrentStop(RouteStop currentStop) { this.currentStop = currentStop; }

    public Boolean getOperationBlocked() { return operationBlocked; }
    public void setOperationBlocked(Boolean operationBlocked) { this.operationBlocked = operationBlocked; }

    public String getTransportInstructions() { return transportInstructions; }
    public void setTransportInstructions(String transportInstructions) { this.transportInstructions = transportInstructions; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
