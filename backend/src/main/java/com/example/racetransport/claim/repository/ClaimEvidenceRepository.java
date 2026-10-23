package com.example.racetransport.claim.repository;

import com.example.racetransport.claim.entity.ClaimEvidence;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimEvidenceRepository extends JpaRepository<ClaimEvidence, Long> {

    @EntityGraph(attributePaths = {"file", "uploadedBy"})
    List<ClaimEvidence> findByClaimIdOrderByUploadedAtAsc(Long claimId);

    long countByClaimId(Long claimId);
}
