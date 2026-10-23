package com.example.racetransport.compliance.entity;

import com.example.racetransport.document.entity.DocumentType;
import com.example.racetransport.transportationrequest.entity.MovementType;
import com.example.racetransport.transportationrequest.entity.TransportPurpose;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Danh mục yêu cầu tuân thủ theo tuyến/loại di chuyển/mục đích (dữ liệu demo, không phải quy định pháp lý chính thức).
 * Bảng: compliance_requirement
 */
@Entity
@Table(name = "compliance_requirement")
public class ComplianceRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    /** NULL = mọi quốc gia */
    @Column(name = "origin_country", length = 2)
    private String originCountry;

    /** NULL = mọi quốc gia */
    @Column(name = "destination_country", length = 2)
    private String destinationCountry;

    /** NULL = mọi loại */
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", length = 40)
    private MovementType movementType;

    /** NULL = mọi mục đích */
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 40)
    private TransportPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private ComplianceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private DocumentType documentType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    /** Tài liệu phải còn hiệu lực/được cấp trong N ngày trước khởi hành */
    @Column(name = "validity_days")
    private Integer validityDays;

    @Column(name = "per_horse", nullable = false)
    private Boolean perHorse = Boolean.FALSE;

    @Column(name = "mandatory", nullable = false)
    private Boolean mandatory = Boolean.TRUE;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

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

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getOriginCountry() { return originCountry; }
    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }

    public String getDestinationCountry() { return destinationCountry; }
    public void setDestinationCountry(String destinationCountry) { this.destinationCountry = destinationCountry; }

    public MovementType getMovementType() { return movementType; }
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }

    public TransportPurpose getPurpose() { return purpose; }
    public void setPurpose(TransportPurpose purpose) { this.purpose = purpose; }

    public ComplianceCategory getCategory() { return category; }
    public void setCategory(ComplianceCategory category) { this.category = category; }

    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getValidityDays() { return validityDays; }
    public void setValidityDays(Integer validityDays) { this.validityDays = validityDays; }

    public Boolean getPerHorse() { return perHorse; }
    public void setPerHorse(Boolean perHorse) { this.perHorse = perHorse; }

    public Boolean getMandatory() { return mandatory; }
    public void setMandatory(Boolean mandatory) { this.mandatory = mandatory; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
