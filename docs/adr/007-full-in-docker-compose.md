# ADR-007: Full-in-Docker topology with configuration externalization

## Status
Accepted (Day 6)

## Context
Day 6 required the entire stack to run from one command, reproducibly from a clean checkout. The developer's native Windows PostgreSQL/MongoDB installations had to remain untouched, and containerized services must never talk to `localhost`.

## Decision
- **Docker Compose defines all seven services**: `postgres`, `mongo`, `kafka`, `eureka-server`, `incident-service`, `asset-service`, `notification-service`, with **named volumes** for both databases.
- Each deployable uses a **multi-stage Dockerfile** (Temurin JDK 26 builder running the project's own Maven wrapper → Temurin JRE 26 runtime), so images build without host-side JARs. The wrapper script is CRLF-normalized inside the image because the checkout is on Windows.
- **Configuration externalization** relies purely on Spring relaxed-binding environment variables — no profile-specific config files: `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `SPRING_DATASOURCE_URL/_USERNAME/_PASSWORD`, `SPRING_MONGODB_URI` (Boot 4.1's real property name — *not* `SPRING_DATA_MONGODB_URI`), `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` (the `eureka.*` family has no `spring.` prefix — *not* `SPRING_EUREKA_…`), `CAMPUSOPS_SECURITY_JWT_SECRET`, and SLA tuning variables.
- **Kafka runs dual listeners**: `PLAINTEXT://kafka:9092` for containers, `PLAINTEXT_HOST://localhost:9092` preserved for host tooling.
- Host port mapping avoids native conflicts: databases map to **5433 / 27018**; application ports stay 8082–8084 / 8761.
- Startup ordering via Compose healthchecks (`depends_on: condition: service_healthy`) using real Actuator health endpoints (curl installed in runtime images) or infrastructure-native checks.

## Consequences
- One-command reproducible stack; fresh containerized data fully isolated from native dev data.
- Two env-var naming traps cost debugging time and are now documented in README + this ADR.
- Postgres 18 requires its volume mounted at `/var/lib/postgresql` (image convention change) — discovered at first boot.
- Single-node topology everywhere; operational simplicity traded against resilience.

## Alternatives considered / rejected
- **Hybrid mode** (container services → native DBs via `host.docker.internal`) — keeps existing dev data but reduces portability and muddies the "one command" story.
- **Copy-prebuilt-JAR images** — faster locally but depend on host build state; multi-stage keeps images reproducible from a clean checkout.
- **Kubernetes** — explicitly out of v1 scope.
