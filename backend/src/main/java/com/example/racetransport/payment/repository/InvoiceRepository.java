package com.example.racetransport.payment.repository;

import com.example.racetransport.payment.entity.Invoice;
import com.example.racetransport.payment.entity.InvoiceStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByQuoteId(Long quoteId);

    Optional<Invoice> findFirstByRequestIdAndStatusNotOrderByIdDesc(Long requestId, InvoiceStatus status);

    @EntityGraph(attributePaths = {"customer", "request"})
    @Query("""
            select i from Invoice i
            where (:customerId is null or i.customer.id = :customerId)
              and i.status in :statuses
            """)
    Page<Invoice> search(@Param("customerId") Long customerId, @Param("statuses") Collection<InvoiceStatus> statuses,
                         Pageable pageable);

    List<Invoice> findByStatusIn(Collection<InvoiceStatus> statuses);
}
