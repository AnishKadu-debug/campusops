package com.campusops.incident.security;

public record CurrentUser(String subject, UserRole role) {
}
