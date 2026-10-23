package com.example.racetransport.handover.repository;

import com.example.racetransport.handover.entity.Handover;
import com.example.racetransport.handover.entity.HandoverStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HandoverRepository extends JpaRepository<Handover, Long> {

    @EntityGraph(attributePaths = {"horse", "createdBy", "confirmedBy"})
    List<Handover> findByTripIdOrderByIdAsc(Long tripId);

    Optional<Handover> findByTripIdAndHorseId(Long tripId, Long horseId);

    long countByTripIdAndStatus(Long tripId, HandoverStatus status);

    @EntityGraph(attributePaths = {"trip", "trip.request", "horse"})
    @Query("""
            select h from Handover h
            where h.status in :statuses
              and (:customerId is null or h.trip.request.customer.id = :customerId)
              and (:staffId is null or exists (select a.id from TripStaffAssignment a
                                               where a.trip = h.trip and a.staff.id = :staffId))
            order by h.handoverTime desc
            """)
    List<Handover> search(@Param("statuses") Collection<HandoverStatus> statuses,
                          @Param("customerId") Long customerId, @Param("staffId") Long staffId);

    @EntityGraph(attributePaths = {"trip"})
    List<Handover> findByHorseIdOrderByHandoverTimeDesc(Long horseId);
}
