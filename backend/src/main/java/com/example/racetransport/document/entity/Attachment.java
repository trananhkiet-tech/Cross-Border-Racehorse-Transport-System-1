package com.example.racetransport.document.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Ảnh/tệp đính kèm cho quan sát sức khỏe, sự cố, bàn giao, tư vấn thú y.
 * Bảng: attachment
 */
@Entity
@Table(name = "attachment")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private StoredFile file;

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false, length = 40)
    private AttachmentRefType refType;

    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Column(name = "caption", length = 255)
    private String caption;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public StoredFile getFile() { return file; }
    public void setFile(StoredFile file) { this.file = file; }

    public AttachmentRefType getRefType() { return refType; }
    public void setRefType(AttachmentRefType refType) { this.refType = refType; }

    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
