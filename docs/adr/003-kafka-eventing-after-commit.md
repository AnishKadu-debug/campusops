# ADR-003: Kafka eventing with AFTER_COMMIT publishing

## Status
Accepted (Days 3–4)

## Context
When something happens in Incident Service (created, assigned, SLA breached), other services must react without blocking or even knowing about the original request. ZooKeeper-free operation was preferred for v1 operational simplicity.

## Decision
- **Apache Kafka 4.0 in KRaft mode** (no ZooKeeper).
- Topics follow a `<domain>.<event>.v1` convention: `incident.created.v1`, `incident.assigned.v1`, `incident.sla-breached.v1`. The `.v1` suffix reserves room for incompatible payload evolution.
- Domain services publish **Spring application events inside their transaction**; a separate `IncidentEventPublisher` listens with `@TransactionalEventListener(phase = AFTER_COMMIT)` and forwards to `KafkaTemplate` only after the database commit succeeds.
- Producer sends with `acks=all`, key = incident id (ordering per incident). Type headers are disabled; consumers bind explicit local DTO types (`spring.json.value.default.type`) so services stay decoupled from producer classes.
- Consumer side uses `ErrorHandlingDeserializer` + Jackson, consumer group `notification-service`.

## Consequences
- No phantom notifications for rolled-back transactions.
- At-least-once delivery: consumers must tolerate redelivery (notification inserts are naturally idempotent-tolerant in v1).
- Event contracts are duplicated as small DTOs per service — deliberate decoupling cost.
- The scheduled scanner publishes from its own transaction, which is what makes the AFTER_COMMIT bridge work there too.

## Alternatives considered / rejected
- **In-process Spring events only** — lost on crash, no cross-service reach.
- **Synchronous REST notification calls** — reintroduces temporal coupling ADR-002 separates out.
- **Publishing before/at transaction time** — risks notifying about incidents that never committed.
