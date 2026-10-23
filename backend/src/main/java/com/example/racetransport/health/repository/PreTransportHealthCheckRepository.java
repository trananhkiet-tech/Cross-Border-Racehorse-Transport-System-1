package com.example.racetransport.health.repository;

import com.example.racetransport.health.entity.PreTransportHealthCheck;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreTransportHealthCheckRepository extends JpaRepository<PreTransportHealthCheck, Long> {

    @EntityGraph(attributePaths = {"horse", "checkedBy"})
    List<PreTransportHealthCheck> findByTripIdOrderByCheckedAtAsc(Long tripId);

    Optional<PreTransportHealthCheck> findByTripIdAndHorseId(Long tripId, Long horseId);

    @EntityGraph(attributePaths = {"trip", "checkedBy"})
    List<PreTransportHealthCheck> findByHorseIdOrderByCheckedAtDesc(Long horseId);
}
