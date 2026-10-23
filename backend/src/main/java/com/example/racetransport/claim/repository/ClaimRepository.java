package com.example.racetransport.claim.repository;

import com.example.racetransport.claim.entity.Claim;
import com.example.racetransport.claim.entity.ClaimStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    @EntityGraph(attributePaths = {"request", "customer", "incident", "horse"})
    @Query("""
            select c from Claim c
            where c.status in :statuses and (:customerId is null or c.customer.id = :customerId)
            """)
    Page<Claim> search(@Param("statuses") Collection<ClaimStatus> statuses, @Param("customerId") Long customerId,
                       Pageable pageable);

    long countByStatusIn(Collection<ClaimStatus> statuses);

    long countByCustomerIdAndStatusIn(Long customerId, Collection<ClaimStatus> statuses);

    List<Claim> findByStatusIn(Collection<ClaimStatus> statuses);

    List<Claim> findByIncidentId(Long incidentId);
}
