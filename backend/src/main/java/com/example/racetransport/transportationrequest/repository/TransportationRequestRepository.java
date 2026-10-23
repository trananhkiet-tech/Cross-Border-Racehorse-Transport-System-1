package com.example.racetransport.transportationrequest.repository;

import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransportationRequestRepository extends JpaRepository<TransportationRequest, Long> {

    @EntityGraph(attributePaths = "customer")
    @Query("""
            select r from TransportationRequest r
            where (:customerId is null or r.customer.id = :customerId)
              and r.status in :statuses
              and (:q is null or lower(r.requestNumber) like lower(concat('%', :q, '%'))
                   or lower(r.customer.fullName) like lower(concat('%', :q, '%'))
                   or lower(r.destinationAddress) like lower(concat('%', :q, '%'))
                   or lower(r.originAddress) like lower(concat('%', :q, '%')))
            """)
    Page<TransportationRequest> search(@Param("customerId") Long customerId,
                                       @Param("statuses") Collection<RequestStatus> statuses,
                                       @Param("q") String q, Pageable pageable);

    long countByStatus(RequestStatus status);

    long countByStatusIn(Collection<RequestStatus> statuses);

    long countByCustomerIdAndStatusIn(Long customerId, Collection<RequestStatus> statuses);

    List<TransportationRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<TransportationRequest> findByStatusInOrderByRequestedDepartureDateAsc(Collection<RequestStatus> statuses);
}
