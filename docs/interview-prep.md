# CampusOps — Interview Preparation

## 30-second pitch

> "CampusOps is a microservices platform for managing campus facility incidents — broken projectors, Wi-Fi outages, AC failures. Students report incidents, each incident gets a priority-based SLA deadline, managers assign technicians, and a scheduled scanner detects SLA breaches. Services communicate synchronously over Eureka and Feign with a circuit breaker for the one request/response dependency, and asynchronously over Kafka for notifications. It's secured with JWT resource servers plus role- and ownership-based authorization, and the entire stack — three services, two PostgreSQL databases, MongoDB, Kafka in KRaft mode — runs from a single Docker Compose command."

## 2-minute architecture explanation

**Services and boundaries.** Three business services with deliberately separate bounded contexts: Incident Service owns the incident lifecycle and state machine (OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED), Asset Service owns heterogeneous campus equipment data, Notification Service is an event-driven consumer that stores notification history. A Eureka server provides discovery. *(ADR-002)*

**Databases — polyglot on purpose.** Incident and notification data are relational and transactional (state transitions, deadlines, audit timestamps), so they use PostgreSQL. Assets have genuinely different shapes per type — a projector has resolution and lamp hours, a router has IP and firmware — so Asset Service uses MongoDB with flexible attributes. *(ADR-001)*

**Synchronous communication.** When creating an incident that references an asset, Incident Service needs an immediate existence answer, so it calls Asset Service via OpenFeign, discovered through Eureka. That call sits behind a Resilience4j circuit breaker with a fallback, so an asset outage degrades incident creation instead of cascading. *(ADR-002)*

**Asynchronous communication.** Everything else is events: IncidentCreated, IncidentAssigned, SlaBreached go to Kafka topics named `<domain>.<event>.v1`. Events are published through Spring's `@TransactionalEventListener(AFTER_COMMIT)` — only after the database commit succeeds, so we never notify about transactions that rolled back. Kafka runs in KRaft mode, no ZooKeeper. *(ADR-003)*

**Security.** Both API-facing services are OAuth2 resource servers validating HS256 JWTs against a shared development secret. The token subject is the only trusted identity — request-body identity fields are ignored — and authorization combines coarse `@PreAuthorize` role gates with service-layer ownership checks: students see their own incidents, technicians see assigned ones, managers see everything, and assignment is manager-only. There's deliberately no auth server in v1; tokens come from a local development mint utility. *(ADR-004, ADR-005)*

**SLA engine.** Each priority maps to a configured duration; the deadline is stored at creation and recalculated when priority changes. A plain Spring scheduled scanner queries for active incidents past deadline without a breach marker, marks them in the same transaction, and publishes the event — the marker makes breach notification exactly-once per incident. *(ADR-006)*

**Deployment.** The whole stack is Docker Compose: multi-stage builds from clean checkout, databases in named volumes, service discovery by container name, configuration entirely through relaxed-binding environment variables, healthcheck-gated startup. *(ADR-007)*

---

## "Why did you choose X?"

### Data & communication
- **Why PostgreSQL + MongoDB?** → Fit-for-purpose: transactional lifecycle data vs schema-flexible equipment attributes. Not uniformity for its own sake. *(ADR-001)*
- **Why Kafka?** → Notifications must react to facts without coupling the producer; durable replayable log beats point-to-point messaging for event fan-out. KRaft avoids running ZooKeeper. *(ADR-003)*
- **Why asynchronous notifications?** → The reporter doesn't need the notification delivered inside the HTTP request; async removes temporal coupling and isolates notification failures.
- **Why `.v1` topic names?** — Payload evolution insurance; a breaking contract change moves to `.v2` instead of silently corrupting consumers.

### Resilience
- **Why Eureka?** → Location transparency: services find each other by logical name; instances can move or scale without config edits. *(ADR-002)*
- **Why Feign?** → Declarative clients over Ribbon-era boilerplate; integrates with Eureka load balancing and interceptors (we propagate JWTs through one).
- **Why Resilience4j?** → The Feign dependency must not cascade failures. Circuit breaker + fallback turns "asset service down" into degraded-but-working incident creation.
- **Why AFTER_COMMIT publishing?** → Publishing inside a transaction risks notifying about rolled-back work; the AFTER_COMMIT phase guarantees only committed facts reach Kafka. *(ADR-003)*

### Security
- **Why JWT?** → Stateless auth fits microservices: each service validates tokens locally without a central session store.
- **Why HS256?** → One shared secret is the simplest thing that provides real signature validation for two internal services in v1.
- **Why a shared secret (and its honest cost)?** → No key-distribution machinery needed at this scale; production would move to RS256 with an IdP-managed public key. Documented as a limitation, not a virtue. *(ADR-004)*
- **Why no auth service?** → Approved v1 scope excluded new microservices; inventing a login endpoint with a fake user store inside a bounded context would be worse architecture than honest dev-token minting.
- **Why service-layer authorization instead of annotations everywhere?** → Ownership rules ("student sees *their own* incident") need entity state; URL/role annotations can't express them. Annotations handle coarse gates (`assign = MANAGER`), the service layer handles the rest. *(ADR-005)*
- **Why force reporterId from the JWT?** → The pre-Day-5 design trusted client-asserted identity — a real vulnerability. The token subject is the only trustworthy source.

### SLA engine
- **Why scheduled scanning instead of timers?** → v1 forbids external scheduling infra; a DB-backed scan survives restarts trivially and needs no per-incident memory of pending timers. *(ADR-006)*
- **Why slaBreachedAt?** → Exactly-once breach events. The scan is idempotent because already-marked incidents are filtered out — restarts and repeated scans can't double-notify.

### Deployment
- **Why Docker Compose (not Kubernetes)?** → The deliverable was a reproducible single-command stack for a handful of services; K8s would add operational complexity with nothing to operate yet. *(ADR-007)*
- **Why multi-stage builds?** → Images reproducible from a clean checkout — no dependence on host build state; dependency layer caching keeps rebuilds fast.
- **Why env vars instead of profile files?** → One artifact, many environments; Spring relaxed binding does the rest. Two naming traps (SPRING_MONGODB_URI vs SPRING_DATA_MONGODB_URI, EUREKA_CLIENT_SERVICEURL_DEFAULTZONE without spring prefix) taught us to verify property namespaces, not assume.
- **Why no Testcontainers?** → Plain JUnit suites against local infra were sufficient at this scale and kept builds fast; Testcontainers is the natural next step for CI portability and was consciously deferred.

---

## Security limitations — be upfront about these

| Current state | What production would look like |
|---|---|
| Tokens minted by a dev utility; no login | Real identity provider (OIDC) issuing tokens after authentication |
| Shared HS256 secret across services | RS256/ES256 asymmetric keys; services hold only public keys |
| No refresh tokens, no expiry strategy beyond 8h dev TTL | Short-lived access tokens + refresh flow |
| No key rotation | Rotating signing keys via JWKS |
| Secrets as documented dev values in config/compose | Vault/secret manager injection; nothing sensitive in Git |
| Notification API unauthenticated | Close the boundary once it stops being purely internal |

The framing that works: *"I know exactly where the security model stops being real, and what replaces each piece in production."*

## Future roadmap (separated from current implementation)

Current implementation: everything described above — nothing on this list exists yet.

1. **Real identity provider + RS256 key management** — replaces the dev mint (biggest gap).
2. **Migration tooling (Flyway/Liquibase)** — Hibernate `ddl-auto=update` already required a manual constraint widening when an enum grew; that pain is the argument.
3. **Testcontainers** — self-contained integration tests for CI.
4. **Centralized secrets** — stop shipping even dev defaults in compose files.
5. **Distributed tracing/metrics** — beyond Actuator health/info once more than a demo depends on it.
6. **Stronger Kafka deployment** — multi-broker, retention/monitoring strategy.
7. **Kubernetes + scaling policies** — when there are real load patterns to scale against.
8. **API gateway** — single entry point/JWT relay if the client surface grows.

Each item maps to a documented limitation — none is speculative feature creep.
