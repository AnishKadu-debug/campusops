package com.campusops.incident.service;

import com.campusops.incident.entity.IncidentPriority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class SlaCalculator {

    private final Duration lowDuration;
    private final Duration mediumDuration;
    private final Duration highDuration;
    private final Duration criticalDuration;

    public SlaCalculator(
            @Value("${campusops.sla.duration.low}") Duration low,
            @Value("${campusops.sla.duration.medium}") Duration medium,
            @Value("${campusops.sla.duration.high}") Duration high,
            @Value("${campusops.sla.duration.critical}") Duration critical) {
        this.lowDuration = low;
        this.mediumDuration = medium;
        this.highDuration = high;
        this.criticalDuration = critical;
    }

    public Instant calculateDeadline(IncidentPriority priority) {
        return Instant.now().plus(durationFor(priority));
    }

    private Duration durationFor(IncidentPriority priority) {
        return switch (priority) {
            case LOW -> lowDuration;
            case MEDIUM -> mediumDuration;
            case HIGH -> highDuration;
            case CRITICAL -> criticalDuration;
        };
    }
}
