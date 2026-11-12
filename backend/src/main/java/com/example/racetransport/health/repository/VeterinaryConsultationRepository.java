package com.example.racetransport.health.repository;

import com.example.racetransport.health.entity.VeterinaryConsultation;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VeterinaryConsultationRepository extends JpaRepository<VeterinaryConsultation, Long> {

    @EntityGraph(attributePaths = {"recordedBy", "horse", "reportFile"})
    List<VeterinaryConsultation> findByIncidentIdOrderByConsultedAtDesc(Long incidentId);

    @EntityGraph(attributePaths = {"recordedBy", "incident"})
    List<VeterinaryConsultation> findByHorseIdOrderByConsultedAtDesc(Long horseId);

    List<VeterinaryConsultation> findByTripIdOrderByConsultedAtDesc(Long tripId);
}
