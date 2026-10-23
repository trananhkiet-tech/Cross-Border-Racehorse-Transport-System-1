package com.example.racetransport.tracking.repository;

import com.example.racetransport.tracking.entity.TrackingEvent;
import com.example.racetransport.tracking.entity.TrackingEventType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {

    @EntityGraph(attributePaths = {"recordedBy", "routeStop"})
    List<TrackingEvent> findByTripIdOrderByEventTimeAscIdAsc(Long tripId);

    Optional<TrackingEvent> findFirstByTripIdOrderByEventTimeDescIdDesc(Long tripId);

    @Query("select coalesce(sum(e.delayMinutes), 0) from TrackingEvent e where e.trip.id = :tripId and e.eventType = :type")
    long sumDelay(@Param("tripId") Long tripId, @Param("type") TrackingEventType type);

    @Query("select coalesce(sum(e.delayMinutes), 0) from TrackingEvent e where e.eventType = :type")
    long sumAllDelay(@Param("type") TrackingEventType type);
}
