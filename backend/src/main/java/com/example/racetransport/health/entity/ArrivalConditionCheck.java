package com.example.racetransport.health.entity;

import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.user.entity.AppUser;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Kiểm tra tình trạng khi đến nơi, trước bàn giao.
 * Bảng: arrival_condition_check
 */
@Entity
@Table(name = "arrival_condition_check", uniqueConstraints = {
        @UniqueConstraint(name = "uq_arrival_condition_check_trip_id_horse_id", columnNames = {"trip_id", "horse_id"})})
public class ArrivalConditionCheck {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false, length = 40)
    private HorseCondition horseCondition;

    @Column(name = "visible_injury", nullable = false)
    private Boolean visibleInjury = Boolean.FALSE;

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

    public HorseCondition getHorseCondition() { return horseCondition; }
    public void setHorseCondition(HorseCondition horseCondition) { this.horseCondition = horseCondition; }

    public Boolean getVisibleInjury() { return visibleInjury; }
    public void setVisibleInjury(Boolean visibleInjury) { this.visibleInjury = visibleInjury; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
