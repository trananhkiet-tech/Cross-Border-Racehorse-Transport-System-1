package com.example.racetransport.health.repository;

import com.example.racetransport.health.entity.ArrivalConditionCheck;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArrivalConditionCheckRepository extends JpaRepository<ArrivalConditionCheck, Long> {

    @EntityGraph(attributePaths = {"horse", "checkedBy"})
    List<ArrivalConditionCheck> findByTripIdOrderByCheckedAtAsc(Long tripId);

    Optional<ArrivalConditionCheck> findByTripIdAndHorseId(Long tripId, Long horseId);

    @EntityGraph(attributePaths = {"trip", "checkedBy"})
    List<ArrivalConditionCheck> findByHorseIdOrderByCheckedAtDesc(Long horseId);
}
