package com.example.racetransport.transportationrequest.entity;

import com.example.racetransport.user.entity.AppUser;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Lịch sử chuyển trạng thái yêu cầu (bao gồm quyết định duyệt/từ chối/yêu cầu bổ sung).
 * Bảng: request_status_history
 */
@Entity
@Table(name = "request_status_history")
public class RequestStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private TransportationRequest request;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 40)
    private RequestStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 40)
    private RequestStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private AppUser changedBy;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TransportationRequest getRequest() { return request; }
    public void setRequest(TransportationRequest request) { this.request = request; }

    public RequestStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(RequestStatus fromStatus) { this.fromStatus = fromStatus; }

    public RequestStatus getToStatus() { return toStatus; }
    public void setToStatus(RequestStatus toStatus) { this.toStatus = toStatus; }

    public AppUser getChangedBy() { return changedBy; }
    public void setChangedBy(AppUser changedBy) { this.changedBy = changedBy; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
