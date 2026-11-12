package com.example.racetransport.feedback.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Đánh giá chuyến của khách sau khi giao (mỗi chuyến một đánh giá). Bảng: trip_feedback */
@Entity
@Table(name = "trip_feedback")
public class TripFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false, unique = true)
    private Long tripId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "punctuality", nullable = false)
    private Integer punctuality;

    @Column(name = "care", nullable = false)
    private Integer care;

    @Column(name = "communication", nullable = false)
    private Integer communication;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Integer getPunctuality() { return punctuality; }
    public void setPunctuality(Integer punctuality) { this.punctuality = punctuality; }
    public Integer getCare() { return care; }
    public void setCare(Integer care) { this.care = care; }
    public Integer getCommunication() { return communication; }
    public void setCommunication(Integer communication) { this.communication = communication; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /** Điểm tổng = trung bình 3 tiêu chí. */
    public double overall() {
        return Math.round((punctuality + care + communication) / 3.0 * 10.0) / 10.0;
    }
}
