package com.example.racetransport.quotation.repository;

import com.example.racetransport.quotation.entity.Quote;
import com.example.racetransport.quotation.entity.QuoteStatus;
import com.example.racetransport.quotation.entity.QuoteType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    Optional<Quote> findFirstByRequestIdAndQuoteTypeOrderByVersionNoDesc(Long requestId, QuoteType type);

    Optional<Quote> findFirstByRequestIdOrderByVersionNoDesc(Long requestId);

    List<Quote> findByRequestIdOrderByVersionNoDesc(Long requestId);

    List<Quote> findByRequestIdAndStatusIn(Long requestId, Collection<QuoteStatus> statuses);

    @EntityGraph(attributePaths = {"request", "request.customer", "createdBy"})
    @Query("""
            select q from Quote q
            where q.quoteType = com.example.racetransport.quotation.entity.QuoteType.FINAL
              and (:customerId is null or q.request.customer.id = :customerId)
              and q.status in :statuses
            """)
    Page<Quote> searchFinal(@Param("customerId") Long customerId, @Param("statuses") Collection<QuoteStatus> statuses,
                            Pageable pageable);

    long countByQuoteTypeAndStatus(QuoteType type, QuoteStatus status);

    @Query("""
            select count(q) from Quote q where q.quoteType = com.example.racetransport.quotation.entity.QuoteType.FINAL
              and q.status = com.example.racetransport.quotation.entity.QuoteStatus.SENT
              and q.request.customer.id = :customerId
            """)
    long countAwaitingCustomer(@Param("customerId") Long customerId);
}
