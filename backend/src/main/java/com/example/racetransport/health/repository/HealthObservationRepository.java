package com.example.racetransport.health.repository;

import com.example.racetransport.health.entity.HealthObservation;
import com.example.racetransport.health.entity.ObservationSeverity;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthObservationRepository extends JpaRepository<HealthObservation, Long> {

    @EntityGraph(attributePaths = {"horse", "author", "incident"})
    List<HealthObservation> findByTripIdOrderByObservedAtDesc(Long tripId);

    @EntityGraph(attributePaths = {"trip", "author"})
    List<HealthObservation> findByHorseIdOrderByObservedAtDesc(Long horseId);

    long countBySeverity(ObservationSeverity severity);
}
