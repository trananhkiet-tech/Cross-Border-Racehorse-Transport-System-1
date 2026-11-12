package com.example.racetransport.health.mapper;

import com.example.racetransport.health.dto.HealthDtos.ArrivalCheckResponse;
import com.example.racetransport.health.dto.HealthDtos.AttachmentRef;
import com.example.racetransport.health.dto.HealthDtos.ObservationResponse;
import com.example.racetransport.health.dto.HealthDtos.PreTransportCheckResponse;
import com.example.racetransport.health.dto.HealthDtos.VetConsultationResponse;
import com.example.racetransport.health.entity.ArrivalConditionCheck;
import com.example.racetransport.health.entity.HealthObservation;
import com.example.racetransport.health.entity.PreTransportHealthCheck;
import com.example.racetransport.health.entity.VeterinaryConsultation;
import java.util.List;

/** Mapping entity → DTO cho module sức khỏe (dùng chung với incident/horse). */
public final class HealthMapper {

    private HealthMapper() { }

    public static PreTransportCheckResponse toPreCheck(PreTransportHealthCheck c) {
        return new PreTransportCheckResponse(c.getId(), c.getTrip().getId(), c.getHorse().getId(),
                c.getHorse().getRegisteredName(), c.getTemperatureC(), c.getGeneralCondition(),
                Boolean.TRUE.equals(c.getIdentityVerified()), Boolean.TRUE.equals(c.getDocumentsPresent()),
                Boolean.TRUE.equals(c.getLoadingOk()), Boolean.TRUE.equals(c.getFitForTravelObserved()), c.getNotes(),
                c.getCheckedBy().getFullName(), c.getCheckedAt());
    }

    public static ObservationResponse toObservation(HealthObservation o, List<AttachmentRef> photos) {
        return new ObservationResponse(o.getId(), o.getTrip().getId(), o.getTrip().getTripNumber(),
                o.getHorse().getId(), o.getHorse().getRegisteredName(), o.getAuthor().getFullName(), o.getObservedAt(),
                o.getLocation(), o.getAppetite(), o.getWaterIntake(), o.getBehavior(), o.getStressLevel(),
                o.getMovement(), Boolean.TRUE.equals(o.getVisibleInjury()), o.getInjuryDescription(),
                o.getTemperatureC(), o.getSeverity(), o.getNotes(),
                o.getIncident() == null ? null : o.getIncident().getId(), photos);
    }

    public static ArrivalCheckResponse toArrival(ArrivalConditionCheck c) {
        return new ArrivalCheckResponse(c.getId(), c.getTrip().getId(), c.getHorse().getId(),
                c.getHorse().getRegisteredName(), c.getHorseCondition(), Boolean.TRUE.equals(c.getVisibleInjury()),
                c.getNotes(), c.getCheckedBy().getFullName(), c.getCheckedAt());
    }

    public static VetConsultationResponse toVet(VeterinaryConsultation v) {
        return new VetConsultationResponse(v.getId(), v.getIncident() == null ? null : v.getIncident().getId(),
                v.getIncident() == null ? null : v.getIncident().getIncidentNumber(),
                v.getTrip() == null ? null : v.getTrip().getId(), v.getTrip() == null ? null : v.getTrip().getTripNumber(),
                v.getHorse() == null ? null : v.getHorse().getId(),
                v.getHorse() == null ? null : v.getHorse().getRegisteredName(), v.getVeterinarianName(), v.getClinic(),
                v.getContact(), v.getConsultedAt(), v.getAdvice(), v.getRecommendedAction(),
                v.getReportFile() == null ? null : v.getReportFile().getId(),
                v.getReportFile() == null ? null : v.getReportFile().getOriginalFilename(),
                v.getRecordedBy().getFullName(), v.getCreatedAt());
    }
}
