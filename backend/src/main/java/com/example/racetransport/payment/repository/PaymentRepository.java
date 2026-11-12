package com.example.racetransport.payment.repository;

import com.example.racetransport.payment.entity.Payment;
import com.example.racetransport.payment.entity.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    @EntityGraph(attributePaths = {"invoice", "invoice.request", "invoice.customer"})
    @Query("""
            select p from Payment p
            where (:customerId is null or p.invoice.customer.id = :customerId)
            """)
    Page<Payment> search(@Param("customerId") Long customerId, Pageable pageable);

    /** Doanh thu thu được theo khoảng thời gian (thanh toán mô phỏng thành công). */
    @Query("""
            select p from Payment p
            where p.status = com.example.racetransport.payment.entity.PaymentStatus.SUCCEEDED
              and p.paidAt >= :from and p.paidAt < :to
            """)
    List<Payment> findSucceededBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByStatus(PaymentStatus status);
}
