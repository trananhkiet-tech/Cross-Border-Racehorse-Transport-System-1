package com.example.racetransport.incident.repository;

import com.example.racetransport.incident.entity.IncidentAction;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentActionRepository extends JpaRepository<IncidentAction, Long> {

    @EntityGraph(attributePaths = {"performedBy", "newRoute", "consultation"})
    List<IncidentAction> findByIncidentIdOrderByPerformedAtAscIdAsc(Long incidentId);
}
