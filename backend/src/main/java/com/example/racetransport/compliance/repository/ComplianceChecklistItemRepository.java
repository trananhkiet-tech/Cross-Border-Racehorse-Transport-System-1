package com.example.racetransport.compliance.repository;

import com.example.racetransport.compliance.entity.ChecklistItemStatus;
import com.example.racetransport.compliance.entity.ComplianceChecklistItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComplianceChecklistItemRepository extends JpaRepository<ComplianceChecklistItem, Long> {

    @Query("""
            select i from ComplianceChecklistItem i
              join fetch i.requirement
              left join fetch i.horse
              left join fetch i.document
              left join fetch i.checkedBy
            where i.request.id = :requestId
            order by i.requirement.category, i.requirement.code, i.id
            """)
    List<ComplianceChecklistItem> findByRequestIdFetch(@Param("requestId") Long requestId);

    List<ComplianceChecklistItem> findByRequestId(Long requestId);

    List<ComplianceChecklistItem> findByDocumentId(Long documentId);

    /** [status, mandatory, count] để tính tiến độ tuân thủ. */
    @Query("""
            select i.status, i.requirement.mandatory, count(i) from ComplianceChecklistItem i
            where i.request.id = :requestId group by i.status, i.requirement.mandatory
            """)
    List<Object[]> countByStatus(@Param("requestId") Long requestId);

    long countByStatusIn(Collection<ChecklistItemStatus> statuses);

    @Query("""
            select count(distinct i.request.id) from ComplianceChecklistItem i
            where i.status in :statuses and i.request.status in
              (com.example.racetransport.transportationrequest.entity.RequestStatus.SUBMITTED,
               com.example.racetransport.transportationrequest.entity.RequestStatus.UNDER_DOCUMENT_REVIEW,
               com.example.racetransport.transportationrequest.entity.RequestStatus.INFORMATION_REQUIRED,
               com.example.racetransport.transportationrequest.entity.RequestStatus.COMPLIANCE_REVIEW)
            """)
    long countRequestsWithItemStatus(@Param("statuses") Collection<ChecklistItemStatus> statuses);

    @Query("""
            select i from ComplianceChecklistItem i
              join fetch i.requirement join fetch i.request r join fetch r.customer left join fetch i.horse
            where i.status in :statuses
              and (:customerId is null or r.customer.id = :customerId)
              and r.status not in (
                com.example.racetransport.transportationrequest.entity.RequestStatus.CANCELLED,
                com.example.racetransport.transportationrequest.entity.RequestStatus.REJECTED,
                com.example.racetransport.transportationrequest.entity.RequestStatus.COMPLETED,
                com.example.racetransport.transportationrequest.entity.RequestStatus.DRAFT)
            order by r.requestedDepartureDate asc
            """)
    List<ComplianceChecklistItem> findOpenItems(@Param("statuses") Collection<ChecklistItemStatus> statuses,
                                                @Param("customerId") Long customerId);
}
