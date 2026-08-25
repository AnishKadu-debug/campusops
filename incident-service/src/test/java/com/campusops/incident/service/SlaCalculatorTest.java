package com.campusops.incident.service;

import com.campusops.incident.entity.IncidentPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SlaCalculatorTest {

    private final SlaCalculator slaCalculator = new SlaCalculator(
            Duration.parse("PT48H"),
            Duration.parse("PT12H"),
            Duration.parse("PT4H"),
            Duration.parse("PT30M"));

    @ParameterizedTest(name = "{0} should produce a deadline {1} from now")
    @CsvSource({
            "LOW, PT48H",
            "MEDIUM, PT12H",
            "HIGH, PT4H",
            "CRITICAL, PT30M"
    })
    @DisplayName("calculateDeadline should map each priority to its configured duration")
    void calculateDeadline_shouldMapPriorityToConfiguredDuration(IncidentPriority priority, String expectedDuration) {
        Instant before = Instant.now();

        Instant deadline = slaCalculator.calculateDeadline(priority);

        Instant after = Instant.now();
        assertThat(deadline).isNotNull();
        assertThat(deadline).isCloseTo(
                before.plus(Duration.parse(expectedDuration)),
                within(Duration.ofSeconds(5)));
        assertThat(deadline).isAfter(after);
    }

    @Test
    @DisplayName("critical deadlines should be nearer than low priority deadlines")
    void calculateDeadline_criticalShouldBeNearerThanLow() {
        Instant criticalDeadline = slaCalculator.calculateDeadline(IncidentPriority.CRITICAL);
        Instant lowDeadline = slaCalculator.calculateDeadline(IncidentPriority.LOW);

        assertThat(criticalDeadline).isBefore(lowDeadline);
    }
}
