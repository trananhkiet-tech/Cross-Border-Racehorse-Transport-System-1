package com.example.racetransport.incident.repository;

import com.example.racetransport.incident.entity.Incident;
import com.example.racetransport.incident.entity.IncidentSeverity;
import com.example.racetransport.incident.entity.IncidentStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @EntityGraph(attributePaths = {"trip", "trip.request", "horse", "reportedBy"})
    @Query("""
            select i from Incident i
            where i.status in :statuses and i.severity in :severities
              and (:tripId is null or i.trip.id = :tripId)
              and (:customerId is null or i.trip.request.customer.id = :customerId)
              and (:staffId is null or exists (select a.id from TripStaffAssignment a
                                               where a.trip = i.trip and a.staff.id = :staffId))
            """)
    Page<Incident> search(@Param("statuses") Collection<IncidentStatus> statuses,
                          @Param("severities") Collection<IncidentSeverity> severities,
                          @Param("tripId") Long tripId, @Param("customerId") Long customerId,
                          @Param("staffId") Long staffId, Pageable pageable);

    @EntityGraph(attributePaths = {"horse", "reportedBy"})
    List<Incident> findByTripIdOrderByReportedAtDesc(Long tripId);

    /** Sự cố mức cao/nghiêm trọng còn mở chưa có quyết định của Manager trên chuyến. */
    @Query("""
            select case when count(i) > 0 then true else false end from Incident i
            where i.trip.id = :tripId and i.id <> :excludeId
              and i.severity in (com.example.racetransport.incident.entity.IncidentSeverity.HIGH,
                                 com.example.racetransport.incident.entity.IncidentSeverity.CRITICAL)
              and i.status not in (com.example.racetransport.incident.entity.IncidentStatus.RESOLVED,
                                   com.example.racetransport.incident.entity.IncidentStatus.CLOSED)
              and (i.decision is null or i.decision = com.example.racetransport.incident.entity.OperationalDecision.STOP)
            """)
    boolean existsOtherBlocking(@Param("tripId") Long tripId, @Param("excludeId") Long excludeId);

    long countByStatusIn(Collection<IncidentStatus> statuses);

    long countByStatusInAndSeverityIn(Collection<IncidentStatus> statuses, Collection<IncidentSeverity> severities);

    List<Incident> findByReportedAtBetween(LocalDateTime from, LocalDateTime to);

    List<Incident> findByHorseIdOrderByReportedAtDesc(Long horseId);
}
