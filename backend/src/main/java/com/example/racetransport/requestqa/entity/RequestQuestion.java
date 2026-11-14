package com.example.racetransport.requestqa.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Câu hỏi/trả lời trên một yêu cầu vận chuyển. Bảng: request_question */
@Entity
@Table(name = "request_question")
public class RequestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "asked_by", nullable = false)
    private Long askedBy;

    @Column(name = "question", nullable = false, length = 1000)
    private String question;

    @Column(name = "answer", length = 2000)
    private String answer;

    @Column(name = "answered_by")
    private Long answeredBy;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Long getAskedBy() { return askedBy; }
    public void setAskedBy(Long askedBy) { this.askedBy = askedBy; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public Long getAnsweredBy() { return answeredBy; }
    public void setAnsweredBy(Long answeredBy) { this.answeredBy = answeredBy; }
    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
