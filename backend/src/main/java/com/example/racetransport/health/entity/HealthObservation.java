package com.example.racetransport.health.entity;

import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.incident.entity.Incident;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.user.entity.AppUser;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Quan sát tình trạng ngựa trong hành trình (Driver/Escort).
 * Bảng: health_observation
 */
@Entity
@Table(name = "health_observation")
public class HealthObservation {

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
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    @Column(name = "location", length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "appetite", nullable = false, length = 40)
    private IntakeLevel appetite;

    @Enumerated(EnumType.STRING)
    @Column(name = "water_intake", nullable = false, length = 40)
    private IntakeLevel waterIntake;

    @Enumerated(EnumType.STRING)
    @Column(name = "behavior", nullable = false, length = 40)
    private HorseBehavior behavior;

    @Enumerated(EnumType.STRING)
    @Column(name = "stress_level", nullable = false, length = 40)
    private StressLevel stressLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement", nullable = false, length = 40)
    private MovementCondition movement;

    @Column(name = "visible_injury", nullable = false)
    private Boolean visibleInjury = Boolean.FALSE;

    @Column(name = "injury_description", length = 500)
    private String injuryDescription;

    @Column(name = "temperature_c", precision = 4, scale = 1)
    private BigDecimal temperatureC;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 40)
    private ObservationSeverity severity;

    @Column(name = "notes", length = 1000)
    private String notes;

    /** Sự cố được tạo từ quan sát (nếu có) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

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

    public AppUser getAuthor() { return author; }
    public void setAuthor(AppUser author) { this.author = author; }

    public LocalDateTime getObservedAt() { return observedAt; }
    public void setObservedAt(LocalDateTime observedAt) { this.observedAt = observedAt; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public IntakeLevel getAppetite() { return appetite; }
    public void setAppetite(IntakeLevel appetite) { this.appetite = appetite; }

    public IntakeLevel getWaterIntake() { return waterIntake; }
    public void setWaterIntake(IntakeLevel waterIntake) { this.waterIntake = waterIntake; }

    public HorseBehavior getBehavior() { return behavior; }
    public void setBehavior(HorseBehavior behavior) { this.behavior = behavior; }

    public StressLevel getStressLevel() { return stressLevel; }
    public void setStressLevel(StressLevel stressLevel) { this.stressLevel = stressLevel; }

    public MovementCondition getMovement() { return movement; }
    public void setMovement(MovementCondition movement) { this.movement = movement; }

    public Boolean getVisibleInjury() { return visibleInjury; }
    public void setVisibleInjury(Boolean visibleInjury) { this.visibleInjury = visibleInjury; }

    public String getInjuryDescription() { return injuryDescription; }
    public void setInjuryDescription(String injuryDescription) { this.injuryDescription = injuryDescription; }

    public BigDecimal getTemperatureC() { return temperatureC; }
    public void setTemperatureC(BigDecimal temperatureC) { this.temperatureC = temperatureC; }

    public ObservationSeverity getSeverity() { return severity; }
    public void setSeverity(ObservationSeverity severity) { this.severity = severity; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
