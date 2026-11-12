package com.example.racetransport.claim.entity;

import com.example.racetransport.user.entity.AppUser;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Nhận định thẩm định claim.
 * Bảng: claim_assessment
 */
@Entity
@Table(name = "claim_assessment")
public class ClaimAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(name = "assessment_note", nullable = false, length = 2000)
    private String assessmentNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation", nullable = false, length = 40)
    private ClaimRecommendation recommendation;

    @Column(name = "recommended_amount", precision = 18, scale = 2)
    private BigDecimal recommendedAmount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessed_by_id", nullable = false)
    private AppUser assessedBy;

    @Column(name = "assessed_at", nullable = false)
    private LocalDateTime assessedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Claim getClaim() { return claim; }
    public void setClaim(Claim claim) { this.claim = claim; }

    public String getAssessmentNote() { return assessmentNote; }
    public void setAssessmentNote(String assessmentNote) { this.assessmentNote = assessmentNote; }

    public ClaimRecommendation getRecommendation() { return recommendation; }
    public void setRecommendation(ClaimRecommendation recommendation) { this.recommendation = recommendation; }

    public BigDecimal getRecommendedAmount() { return recommendedAmount; }
    public void setRecommendedAmount(BigDecimal recommendedAmount) { this.recommendedAmount = recommendedAmount; }

    public AppUser getAssessedBy() { return assessedBy; }
    public void setAssessedBy(AppUser assessedBy) { this.assessedBy = assessedBy; }

    public LocalDateTime getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDateTime assessedAt) { this.assessedAt = assessedAt; }
}
