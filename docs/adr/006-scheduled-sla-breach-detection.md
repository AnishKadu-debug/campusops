# ADR-006: Scheduled SLA breach detection with slaBreachedAt deduplication

## Status
Accepted (Day 4)

## Context
Every incident receives an SLA deadline at creation (priority → configured duration: LOW PT48H, MEDIUM PT12H, HIGH PT4H, CRITICAL PT30M); changing priority recalculates it. Breaches must be detected and turned into `SlaBreachedEvent`s without blocking any request path, and v1 explicitly forbids external scheduling infrastructure.

## Decision
- Plain **Spring scheduling** (`@EnableScheduling`) runs `SlaBreachScanner` on a configurable fixed delay (default 30s).
- Each scan queries active incidents with status `OPEN`/`ASSIGNED`/`IN_PROGRESS`, `slaDeadline < now`, and `sla_breached_at IS NULL` (single derived repository finder).
- For each hit, the scanner marks `slaBreachedAt` **in the same transaction** that publishes the Spring event; the AFTER_COMMIT bridge (ADR-003) then delivers `SlaBreachedEvent` to Kafka topic `incident.sla-breached.v1`.

## Consequences
- Exactly one breach event per incident, even across repeated scans or service restarts — the database marker is the dedup source of truth.
- Detection latency is bounded by the scan interval, which is acceptable for SLA semantics.
- RESOLVED/CLOSED incidents are structurally excluded from scans.
- Raising a priority re-arms detection naturally: the deadline moves out, and an already-marked incident never refires.

## Alternatives considered / rejected
- **Database jobs/triggers** — external infrastructure, forbidden by v1 scope.
- **Per-incident timers (delay queues/schedulers)** — significant complexity for no v1 benefit; doesn't survive restarts without persistence anyway.
- **Kafka Streams / windowed processing** — far beyond need; the marker column already encodes state.
