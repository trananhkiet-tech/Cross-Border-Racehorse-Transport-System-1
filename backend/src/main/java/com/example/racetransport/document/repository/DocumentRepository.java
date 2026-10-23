package com.example.racetransport.document.repository;

import com.example.racetransport.document.entity.Document;
import com.example.racetransport.document.entity.DocumentReviewStatus;
import com.example.racetransport.document.entity.DocumentType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @EntityGraph(attributePaths = {"file", "horse", "reviewer", "uploadedBy"})
    List<Document> findByRequestIdOrderByCreatedAtDesc(Long requestId);

    @EntityGraph(attributePaths = {"file", "request", "reviewer", "uploadedBy"})
    List<Document> findByHorseIdOrderByCreatedAtDesc(Long horseId);

    long countByRequestIdAndReviewStatus(Long requestId, DocumentReviewStatus status);

    long countByReviewStatus(DocumentReviewStatus status);

    /** Danh sách cho Specialist: lọc theo trạng thái, loại tài liệu, từ khóa. */
    @EntityGraph(attributePaths = {"file", "horse", "request", "request.customer", "uploadedBy", "reviewer"})
    @Query(value = """
            select d from Document d
              left join d.request r
              left join r.customer rc
              left join d.horse h
              left join h.owner ho
            where d.reviewStatus in :statuses
              and d.documentType in :types
              and (:customerId is null or rc.id = :customerId or ho.id = :customerId)
              and (:q is null or lower(d.title) like lower(concat('%', :q, '%'))
                   or lower(d.documentNumber) like lower(concat('%', :q, '%'))
                   or lower(d.file.originalFilename) like lower(concat('%', :q, '%')))
            """,
            countQuery = """
            select count(d) from Document d
              left join d.request r
              left join r.customer rc
              left join d.horse h
              left join h.owner ho
            where d.reviewStatus in :statuses
              and d.documentType in :types
              and (:customerId is null or rc.id = :customerId or ho.id = :customerId)
              and (:q is null or lower(d.title) like lower(concat('%', :q, '%'))
                   or lower(d.documentNumber) like lower(concat('%', :q, '%'))
                   or lower(d.file.originalFilename) like lower(concat('%', :q, '%')))
            """)
    Page<Document> search(@Param("statuses") Collection<DocumentReviewStatus> statuses,
                          @Param("types") Collection<DocumentType> types,
                          @Param("customerId") Long customerId, @Param("q") String q, Pageable pageable);

    /** Tài liệu đã duyệt sắp hết hạn trong khoảng [from, to]. */
    @EntityGraph(attributePaths = {"horse", "request"})
    @Query("""
            select d from Document d
            where d.reviewStatus = com.example.racetransport.document.entity.DocumentReviewStatus.APPROVED
              and d.expiryDate is not null and d.expiryDate between :from and :to
            order by d.expiryDate asc
            """)
    List<Document> findExpiring(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Tài liệu đã duyệt nhưng đã quá hạn (để chuyển EXPIRED). */
    @Query("""
            select d from Document d
            where d.reviewStatus = com.example.racetransport.document.entity.DocumentReviewStatus.APPROVED
              and d.expiryDate is not null and d.expiryDate < :today
            """)
    List<Document> findExpiredApproved(@Param("today") LocalDate today);
}
