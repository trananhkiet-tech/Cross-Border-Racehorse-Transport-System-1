package com.example.racetransport.incident.entity;

import com.example.racetransport.health.entity.VeterinaryConsultation;
import com.example.racetransport.route.entity.Route;
import com.example.racetransport.user.entity.AppUser;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Nhật ký hành động xử lý sự cố.
 * Bảng: incident_action
 */
@Entity
@Table(name = "incident_action")
public class IncidentAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private IncidentActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 40)
    private OperationalDecision decision;

    @Column(name = "note", nullable = false, length = 2000)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by_id", nullable = false)
    private AppUser performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id")
    private VeterinaryConsultation consultation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_route_id")
    private Route newRoute;

    @Column(name = "extra_cost", precision = 18, scale = 2)
    private BigDecimal extraCost;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }

    public IncidentActionType getActionType() { return actionType; }
    public void setActionType(IncidentActionType actionType) { this.actionType = actionType; }

    public OperationalDecision getDecision() { return decision; }
    public void setDecision(OperationalDecision decision) { this.decision = decision; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public AppUser getPerformedBy() { return performedBy; }
    public void setPerformedBy(AppUser performedBy) { this.performedBy = performedBy; }

    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }

    public VeterinaryConsultation getConsultation() { return consultation; }
    public void setConsultation(VeterinaryConsultation consultation) { this.consultation = consultation; }

    public Route getNewRoute() { return newRoute; }
    public void setNewRoute(Route newRoute) { this.newRoute = newRoute; }

    public BigDecimal getExtraCost() { return extraCost; }
    public void setExtraCost(BigDecimal extraCost) { this.extraCost = extraCost; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
