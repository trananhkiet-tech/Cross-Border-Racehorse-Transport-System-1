package com.example.racetransport.route.entity;

import com.example.racetransport.transportationrequest.entity.TransportMode;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tuyến đường mẫu, dùng lại cho nhiều chuyến.
 * Bảng: route
 */
@Entity
@Table(name = "route")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_code", nullable = false, unique = true, length = 20)
    private String routeCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "origin_country", nullable = false, length = 2)
    private String originCountry;

    @Column(name = "origin_location", nullable = false, length = 150)
    private String originLocation;

    @Column(name = "destination_country", nullable = false, length = 2)
    private String destinationCountry;

    @Column(name = "destination_location", nullable = false, length = 150)
    private String destinationLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", nullable = false, length = 40)
    private TransportMode transportMode;

    @Column(name = "estimated_distance_km", precision = 10, scale = 1)
    private BigDecimal estimatedDistanceKm;

    @Column(name = "estimated_duration_hours", precision = 8, scale = 1)
    private BigDecimal estimatedDurationHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private RouteStatus status;

    /** Tuyến thay thế cho tuyến gốc */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alternative_of_id")
    private Route alternativeOf;

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

    public String getRouteCode() { return routeCode; }
    public void setRouteCode(String routeCode) { this.routeCode = routeCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOriginCountry() { return originCountry; }
    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }

    public String getOriginLocation() { return originLocation; }
    public void setOriginLocation(String originLocation) { this.originLocation = originLocation; }

    public String getDestinationCountry() { return destinationCountry; }
    public void setDestinationCountry(String destinationCountry) { this.destinationCountry = destinationCountry; }

    public String getDestinationLocation() { return destinationLocation; }
    public void setDestinationLocation(String destinationLocation) { this.destinationLocation = destinationLocation; }

    public TransportMode getTransportMode() { return transportMode; }
    public void setTransportMode(TransportMode transportMode) { this.transportMode = transportMode; }

    public BigDecimal getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public void setEstimatedDistanceKm(BigDecimal estimatedDistanceKm) { this.estimatedDistanceKm = estimatedDistanceKm; }

    public BigDecimal getEstimatedDurationHours() { return estimatedDurationHours; }
    public void setEstimatedDurationHours(BigDecimal estimatedDurationHours) { this.estimatedDurationHours = estimatedDurationHours; }

    public RouteStatus getStatus() { return status; }
    public void setStatus(RouteStatus status) { this.status = status; }

    public Route getAlternativeOf() { return alternativeOf; }
    public void setAlternativeOf(Route alternativeOf) { this.alternativeOf = alternativeOf; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
