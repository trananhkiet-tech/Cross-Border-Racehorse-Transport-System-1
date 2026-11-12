package com.example.racetransport.health.entity;

import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.user.entity.AppUser;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kiểm tra (quan sát) trước khi xếp ngựa lên xe.
 * Bảng: pre_transport_health_check
 */
@Entity
@Table(name = "pre_transport_health_check", uniqueConstraints = {
        @UniqueConstraint(name = "uq_pre_transport_health_check_trip_id_horse_id", columnNames = {"trip_id", "horse_id"})})
public class PreTransportHealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private TransportationTrip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horse_id", nullable = false)
    private Horse horse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checked_by_id", nullable = false)
    private AppUser checkedBy;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @Column(name = "temperature_c", precision = 4, scale = 1)
    private BigDecimal temperatureC;

    @Enumerated(EnumType.STRING)
    @Column(name = "general_condition", nullable = false, length = 40)
    private HorseCondition generalCondition;

    @Column(name = "identity_verified", nullable = false)
    private Boolean identityVerified = Boolean.FALSE;

    @Column(name = "documents_present", nullable = false)
    private Boolean documentsPresent = Boolean.FALSE;

    @Column(name = "loading_ok", nullable = false)
    private Boolean loadingOk = Boolean.FALSE;

    /** Quan sát của đội vận hành, không phải chẩn đoán */
    @Column(name = "fit_for_travel_observed", nullable = false)
    private Boolean fitForTravelObserved = Boolean.FALSE;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TransportationTrip getTrip() { return trip; }
    public void setTrip(TransportationTrip trip) { this.trip = trip; }

    public Horse getHorse() { return horse; }
    public void setHorse(Horse horse) { this.horse = horse; }

    public AppUser getCheckedBy() { return checkedBy; }
    public void setCheckedBy(AppUser checkedBy) { this.checkedBy = checkedBy; }

    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }

    public BigDecimal getTemperatureC() { return temperatureC; }
    public void setTemperatureC(BigDecimal temperatureC) { this.temperatureC = temperatureC; }

    public HorseCondition getGeneralCondition() { return generalCondition; }
    public void setGeneralCondition(HorseCondition generalCondition) { this.generalCondition = generalCondition; }

    public Boolean getIdentityVerified() { return identityVerified; }
    public void setIdentityVerified(Boolean identityVerified) { this.identityVerified = identityVerified; }

    public Boolean getDocumentsPresent() { return documentsPresent; }
    public void setDocumentsPresent(Boolean documentsPresent) { this.documentsPresent = documentsPresent; }

    public Boolean getLoadingOk() { return loadingOk; }
    public void setLoadingOk(Boolean loadingOk) { this.loadingOk = loadingOk; }

    public Boolean getFitForTravelObserved() { return fitForTravelObserved; }
    public void setFitForTravelObserved(Boolean fitForTravelObserved) { this.fitForTravelObserved = fitForTravelObserved; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
