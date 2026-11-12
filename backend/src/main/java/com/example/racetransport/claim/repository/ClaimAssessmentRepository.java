package com.example.racetransport.claim.repository;

import com.example.racetransport.claim.entity.ClaimAssessment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimAssessmentRepository extends JpaRepository<ClaimAssessment, Long> {

    @EntityGraph(attributePaths = "assessedBy")
    List<ClaimAssessment> findByClaimIdOrderByAssessedAtAsc(Long claimId);

    long countByClaimId(Long claimId);
}
