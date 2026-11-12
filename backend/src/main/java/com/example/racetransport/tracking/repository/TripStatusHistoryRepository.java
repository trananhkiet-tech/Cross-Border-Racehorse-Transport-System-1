package com.example.racetransport.tracking.repository;

import com.example.racetransport.tracking.entity.TripStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripStatusHistoryRepository extends JpaRepository<TripStatusHistory, Long> {

    @EntityGraph(attributePaths = "changedBy")
    List<TripStatusHistory> findByTripIdOrderByChangedAtAscIdAsc(Long tripId);
}
