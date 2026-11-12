package com.example.racetransport.claim.repository;

import com.example.racetransport.claim.entity.ClaimSettlement;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimSettlementRepository extends JpaRepository<ClaimSettlement, Long> {

    @EntityGraph(attributePaths = "settledBy")
    List<ClaimSettlement> findByClaimIdOrderBySettledAtAsc(Long claimId);

    @Query("""
            select coalesce(sum(s.amount), 0) from ClaimSettlement s
            where s.claim.id = :claimId and s.status = com.example.racetransport.claim.entity.SettlementStatus.PAID
            """)
    BigDecimal sumPaid(@Param("claimId") Long claimId);
}
