package com.example.racetransport.handover.entity;

import com.example.racetransport.health.entity.ArrivalConditionCheck;
import com.example.racetransport.health.entity.HorseCondition;
import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.user.entity.AppUser;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Biên bản bàn giao từng ngựa tại điểm đến; khách hàng xác nhận.
 * Bảng: handover
 */
@Entity
@Table(name = "handover", uniqueConstraints = {
        @UniqueConstraint(name = "uq_handover_trip_id_horse_id", columnNames = {"trip_id", "horse_id"})})
public class Handover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "handover_number", nullable = false, unique = true, length = 30)
    private String handoverNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private TransportationTrip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horse_id", nullable = false)
    private Horse horse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_check_id")
    private ArrivalConditionCheck arrivalCheck;

    @Column(name = "destination", nullable = false, length = 255)
    private String destination;

    @Column(name = "handover_time", nullable = false)
    private LocalDateTime handoverTime;

    @Column(name = "receiver_name", nullable = false, length = 150)
    private String receiverName;

    @Column(name = "receiver_contact", length = 150)
    private String receiverContact;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false, length = 40)
    private HorseCondition horseCondition;

    @Column(name = "documents_handed_over", nullable = false)
    private Boolean documentsHandedOver = Boolean.FALSE;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private HandoverStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private AppUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_id")
    private AppUser confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "customer_comment", length = 1000)
    private String customerComment;

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

    public String getHandoverNumber() { return handoverNumber; }
    public void setHandoverNumber(String handoverNumber) { this.handoverNumber = handoverNumber; }

    public TransportationTrip getTrip() { return trip; }
    public void setTrip(TransportationTrip trip) { this.trip = trip; }

    public Horse getHorse() { return horse; }
    public void setHorse(Horse horse) { this.horse = horse; }

    public ArrivalConditionCheck getArrivalCheck() { return arrivalCheck; }
    public void setArrivalCheck(ArrivalConditionCheck arrivalCheck) { this.arrivalCheck = arrivalCheck; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public LocalDateTime getHandoverTime() { return handoverTime; }
    public void setHandoverTime(LocalDateTime handoverTime) { this.handoverTime = handoverTime; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverContact() { return receiverContact; }
    public void setReceiverContact(String receiverContact) { this.receiverContact = receiverContact; }

    public HorseCondition getHorseCondition() { return horseCondition; }
    public void setHorseCondition(HorseCondition horseCondition) { this.horseCondition = horseCondition; }

    public Boolean getDocumentsHandedOver() { return documentsHandedOver; }
    public void setDocumentsHandedOver(Boolean documentsHandedOver) { this.documentsHandedOver = documentsHandedOver; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public HandoverStatus getStatus() { return status; }
    public void setStatus(HandoverStatus status) { this.status = status; }

    public AppUser getCreatedBy() { return createdBy; }
    public void setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; }

    public AppUser getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(AppUser confirmedBy) { this.confirmedBy = confirmedBy; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public String getCustomerComment() { return customerComment; }
    public void setCustomerComment(String customerComment) { this.customerComment = customerComment; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
