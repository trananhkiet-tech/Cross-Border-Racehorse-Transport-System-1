package com.example.racetransport.compliance.entity;

import com.example.racetransport.document.entity.Document;
import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.user.entity.AppUser;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Checklist tuân thủ sinh cho từng yêu cầu.
 * Bảng: compliance_checklist_item
 */
@Entity
@Table(name = "compliance_checklist_item", uniqueConstraints = {
        @UniqueConstraint(name = "uq_compliance_checklist_item_request_id_requirement_id_horse_id", columnNames = {"request_id", "requirement_id", "horse_id"})})
public class ComplianceChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private TransportationRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id", nullable = false)
    private ComplianceRequirement requirement;

    /** NULL nếu yêu cầu áp dụng cho cả lô */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horse_id")
    private Horse horse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ChecklistItemStatus status;

    /** Tài liệu minh chứng hiện hành */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(name = "reviewer_note", length = 1000)
    private String reviewerNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by_id")
    private AppUser checkedBy;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

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

    public TransportationRequest getRequest() { return request; }
    public void setRequest(TransportationRequest request) { this.request = request; }

    public ComplianceRequirement getRequirement() { return requirement; }
    public void setRequirement(ComplianceRequirement requirement) { this.requirement = requirement; }

    public Horse getHorse() { return horse; }
    public void setHorse(Horse horse) { this.horse = horse; }

    public ChecklistItemStatus getStatus() { return status; }
    public void setStatus(ChecklistItemStatus status) { this.status = status; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public String getReviewerNote() { return reviewerNote; }
    public void setReviewerNote(String reviewerNote) { this.reviewerNote = reviewerNote; }

    public AppUser getCheckedBy() { return checkedBy; }
    public void setCheckedBy(AppUser checkedBy) { this.checkedBy = checkedBy; }

    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
