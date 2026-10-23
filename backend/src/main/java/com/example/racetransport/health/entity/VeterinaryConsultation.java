package com.example.racetransport.health.entity;

import com.example.racetransport.document.entity.StoredFile;
import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.incident.entity.Incident;
import com.example.racetransport.incident.entity.OperationalDecision;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.user.entity.AppUser;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bản ghi tư vấn từ bác sĩ thú y bên ngoài (không phải tài khoản hệ thống).
 * Bảng: veterinary_consultation
 */
@Entity
@Table(name = "veterinary_consultation")
public class VeterinaryConsultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private TransportationTrip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horse_id")
    private Horse horse;

    @Column(name = "veterinarian_name", nullable = false, length = 150)
    private String veterinarianName;

    @Column(name = "clinic", length = 150)
    private String clinic;

    @Column(name = "contact", length = 150)
    private String contact;

    @Column(name = "consulted_at", nullable = false)
    private LocalDateTime consultedAt;

    @Column(name = "advice", nullable = false, length = 2000)
    private String advice;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", length = 40)
    private OperationalDecision recommendedAction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_file_id")
    private StoredFile reportFile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_id", nullable = false)
    private AppUser recordedBy;

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

    public TransportationTrip getTrip() { return trip; }
    public void setTrip(TransportationTrip trip) { this.trip = trip; }

    public Horse getHorse() { return horse; }
    public void setHorse(Horse horse) { this.horse = horse; }

    public String getVeterinarianName() { return veterinarianName; }
    public void setVeterinarianName(String veterinarianName) { this.veterinarianName = veterinarianName; }

    public String getClinic() { return clinic; }
    public void setClinic(String clinic) { this.clinic = clinic; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public LocalDateTime getConsultedAt() { return consultedAt; }
    public void setConsultedAt(LocalDateTime consultedAt) { this.consultedAt = consultedAt; }

    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }

    public OperationalDecision getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(OperationalDecision recommendedAction) { this.recommendedAction = recommendedAction; }

    public StoredFile getReportFile() { return reportFile; }
    public void setReportFile(StoredFile reportFile) { this.reportFile = reportFile; }

    public AppUser getRecordedBy() { return recordedBy; }
    public void setRecordedBy(AppUser recordedBy) { this.recordedBy = recordedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
