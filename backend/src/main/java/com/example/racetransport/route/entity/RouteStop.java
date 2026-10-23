package com.example.racetransport.route.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Điểm dừng theo thứ tự trên tuyến.
 * Bảng: route_stop
 */
@Entity
@Table(name = "route_stop", uniqueConstraints = {
        @UniqueConstraint(name = "uq_route_stop_route_id_stop_order", columnNames = {"route_id", "stop_order"})})
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_type", nullable = false, length = 40)
    private StopType stopType;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    /** Phút kể từ giờ khởi hành dự kiến */
    @Column(name = "planned_arrival_offset_min")
    private Integer plannedArrivalOffsetMin;

    @Column(name = "planned_duration_min")
    private Integer plannedDurationMin;

    @Column(name = "instructions", length = 1000)
    private String instructions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }

    public Integer getStopOrder() { return stopOrder; }
    public void setStopOrder(Integer stopOrder) { this.stopOrder = stopOrder; }

    public StopType getStopType() { return stopType; }
    public void setStopType(StopType stopType) { this.stopType = stopType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public Integer getPlannedArrivalOffsetMin() { return plannedArrivalOffsetMin; }
    public void setPlannedArrivalOffsetMin(Integer plannedArrivalOffsetMin) { this.plannedArrivalOffsetMin = plannedArrivalOffsetMin; }

    public Integer getPlannedDurationMin() { return plannedDurationMin; }
    public void setPlannedDurationMin(Integer plannedDurationMin) { this.plannedDurationMin = plannedDurationMin; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}
