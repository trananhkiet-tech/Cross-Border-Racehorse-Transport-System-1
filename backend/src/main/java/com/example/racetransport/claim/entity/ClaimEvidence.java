package com.example.racetransport.claim.entity;

import com.example.racetransport.document.entity.StoredFile;
import com.example.racetransport.user.entity.AppUser;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bằng chứng cho claim.
 * Bảng: claim_evidence
 */
@Entity
@Table(name = "claim_evidence")
public class ClaimEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private StoredFile file;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 40)
    private EvidenceType evidenceType;

    @Column(name = "notes", length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private AppUser uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Claim getClaim() { return claim; }
    public void setClaim(Claim claim) { this.claim = claim; }

    public StoredFile getFile() { return file; }
    public void setFile(StoredFile file) { this.file = file; }

    public EvidenceType getEvidenceType() { return evidenceType; }
    public void setEvidenceType(EvidenceType evidenceType) { this.evidenceType = evidenceType; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public AppUser getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(AppUser uploadedBy) { this.uploadedBy = uploadedBy; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
