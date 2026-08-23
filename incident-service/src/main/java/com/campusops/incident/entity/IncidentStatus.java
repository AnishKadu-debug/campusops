package com.campusops.incident.entity;

public enum IncidentStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    public boolean canTransitionTo(IncidentStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case OPEN -> target == ASSIGNED;
            case ASSIGNED -> target == IN_PROGRESS;
            case IN_PROGRESS -> target == RESOLVED;
            case RESOLVED -> target == CLOSED;
            case CLOSED -> false;
        };
    }
}
