package com.example.racetransport.health;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.racetransport.health.dto.HealthDtos.ObservationRequest;
import com.example.racetransport.health.entity.HorseBehavior;
import com.example.racetransport.health.entity.IntakeLevel;
import com.example.racetransport.health.entity.MovementCondition;
import com.example.racetransport.health.entity.ObservationSeverity;
import com.example.racetransport.health.entity.StressLevel;
import com.example.racetransport.health.service.HealthService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Mức độ quan sát do backend suy ra (không phải chẩn đoán). */
class ObservationSeverityTest {

    private static ObservationRequest obs(IntakeLevel food, IntakeLevel water, HorseBehavior behavior, StressLevel stress,
                                          MovementCondition movement, boolean injury, String temp) {
        return new ObservationRequest(1L, 1L, null, null, food, water, behavior, stress, movement, injury,
                injury ? "Vết xước" : null, temp == null ? null : new BigDecimal(temp), null, null, false);
    }

    @Test
    void normalSignsAreNormal() {
        assertEquals(ObservationSeverity.NORMAL, HealthService.computeSeverity(obs(IntakeLevel.NORMAL, IntakeLevel.NORMAL,
                HorseBehavior.CALM, StressLevel.LOW, MovementCondition.NORMAL, false, "37.8")));
    }

    @Test
    void reducedAppetiteNeedsAttention() {
        assertEquals(ObservationSeverity.ATTENTION, HealthService.computeSeverity(obs(IntakeLevel.REDUCED, IntakeLevel.NORMAL,
                HorseBehavior.CALM, StressLevel.LOW, MovementCondition.NORMAL, false, null)));
    }

    @Test
    void visibleInjuryOrLamenessIsUrgent() {
        assertEquals(ObservationSeverity.URGENT, HealthService.computeSeverity(obs(IntakeLevel.NORMAL, IntakeLevel.NORMAL,
                HorseBehavior.CALM, StressLevel.LOW, MovementCondition.NORMAL, true, null)));
        assertEquals(ObservationSeverity.URGENT, HealthService.computeSeverity(obs(IntakeLevel.NORMAL, IntakeLevel.NORMAL,
                HorseBehavior.CALM, StressLevel.LOW, MovementCondition.LAME, false, null)));
    }

    @Test
    void agitatedWithHighStressIsUrgent() {
        assertEquals(ObservationSeverity.URGENT, HealthService.computeSeverity(obs(IntakeLevel.NONE, IntakeLevel.REDUCED,
                HorseBehavior.AGITATED, StressLevel.HIGH, MovementCondition.STIFF, false, "38.9")));
    }

    @Test
    void highTemperatureIsUrgent() {
        assertEquals(ObservationSeverity.URGENT, HealthService.computeSeverity(obs(IntakeLevel.NORMAL, IntakeLevel.NORMAL,
                HorseBehavior.CALM, StressLevel.LOW, MovementCondition.NORMAL, false, "39.6")));
    }
}
