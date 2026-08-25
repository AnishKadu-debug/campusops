package com.campusops.incident.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentPriority priority;

    @Column(name = "asset_id", length = 100)
    private String assetId;

    @Column(name = "reporter_id", nullable = false, length = 100)
    private String reporterId;

    @Column(name = "assignee_id", length = 100)
    private String assigneeId;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "sla_breached_at")
    private Instant slaBreachedAt;

    @Column(nullable = false)
    private boolean active;
}
