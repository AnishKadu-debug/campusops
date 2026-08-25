# CampusOps — Project Context

## 1. Project Overview

CampusOps is a production-oriented microservices-based campus incident
management platform.

The system allows students/faculty to report operational incidents such as:

- Broken projectors
- AC failures
- Wi-Fi outages
- Damaged lab equipment

The system associates incidents with campus assets, determines priority
and SLA deadlines, manages the incident lifecycle, publishes events,
processes notifications asynchronously, and detects SLA breaches.

The project is intentionally scoped for a short development timeline.
Do not add technologies or features simply because they are common in
production systems.

The primary goals are:

1. Understanding
2. Correctness
3. Practical architecture
4. Maintainability
5. Interview value

---

# 2. Architecture

CampusOps consists of three business microservices.

## Incident Service

Port: 8082

Responsibilities:

- Incident management
- Incident CRUD
- Incident lifecycle/state machine
- Priority handling
- SLA calculation
- SLA breach detection
- Event publishing
- User authorization

Database:

- PostgreSQL

Communication:

- OpenFeign + Eureka → Asset Service
- Kafka producer → Notification Service

---

## Asset Service

Port: 8083

Responsibilities:

- Manage campus assets
- Asset CRUD
- Asset-specific information

Examples:

- Projector
- Router
- AC
- Lab equipment

Database:

- MongoDB

Communication:

- Registers with Eureka
- Called by Incident Service through OpenFeign

---

## Notification Service

Port: 8084

Responsibilities:

- Consume incident-related Kafka events
- Create/store notification records
- Provide notification history if required

Database:

- PostgreSQL

Communication:

- Kafka consumer

Real email/SMS delivery is intentionally out of scope for v1.

---

# 3. Infrastructure

## Eureka Server

Port: 8761

Purpose:

- Service discovery
- Allows services to discover each other by service name
- Avoids hardcoded service URLs

Incident Service and Asset Service register with Eureka.

---

## Apache Kafka

Kafka is used for asynchronous event communication.

Kafka runs in KRaft mode.

ZooKeeper is intentionally not used.

Initial events:

- IncidentCreated
- IncidentAssigned
- SlaBreached

Kafka is primarily used where another service needs to react to an
event without blocking the original request.

---

# 4. Communication Rules

Use synchronous communication when the caller requires an immediate
response.

Example:

Incident Service → OpenFeign → Asset Service

Use asynchronous communication when an event has occurred and other
services can react independently.

Example:

Incident Service → Kafka → Notification Service

Do not replace Kafka with REST merely for convenience when the
communication is intentionally asynchronous.

Do not replace Feign with Kafka when the Incident Service requires an
immediate response from Asset Service.

---

# 5. Resilience

Incident Service uses Resilience4j for communication with Asset Service.

Expected flow:

Incident Service
→ Feign
→ Asset Service

If Asset Service becomes unavailable:

Feign failure/timeout
→ Resilience4j Circuit Breaker
→ fallback

The implementation should prevent a failure in Asset Service from
causing uncontrolled cascading failures.

Do not introduce additional resilience technologies unless explicitly
approved.

---

# 6. Incident Lifecycle

The incident lifecycle is:

OPEN
  ↓
ASSIGNED
  ↓
IN_PROGRESS
  ↓
RESOLVED
  ↓
CLOSED

Only valid transitions are allowed.

Examples of invalid transitions:

OPEN → CLOSED
OPEN → RESOLVED
CLOSED → IN_PROGRESS

The state transition rules must be enforced by business logic rather
than allowing arbitrary status updates from the API.

The state machine is a business-critical part of the system and must
have unit tests.

---

# 7. SLA Engine

Each incident has a priority.

Priority determines the SLA duration.

The SLA engine:

1. Receives the incident priority.
2. Determines the configured SLA duration.
3. Calculates the SLA deadline.
4. Stores the deadline with the incident.
5. Periodically checks for breached incidents.
6. Publishes SlaBreached when an SLA breach is detected.

A scheduled job is used for breach detection in v1.

Do not introduce external scheduling infrastructure.

---

# 8. Security

Authentication:

- JWT

Authorization:

- Role-Based Access Control

Roles:

- STUDENT
- TECHNICIAN
- MANAGER

Authorization rules:

STUDENT:
- Can access their own incidents.

TECHNICIAN:
- Can access incidents assigned to them.

MANAGER:
- Can access all incidents permitted by the business rules.

Do not implement authentication without authorization.

Do not treat possession of a valid JWT as sufficient authorization.

---

# 9. Databases

## PostgreSQL

Used by:

- Incident Service
- Notification Service

Reason:

Incident data is relational and transactional.

Relationships, constraints and ACID transactions are important.

---

## MongoDB

Used by:

- Asset Service

Reason:

Different asset types can have genuinely different fields.

Example:

Projector:
- resolution
- lampHours

Router:
- ip
- firmware
- ports

MongoDB is therefore a deliberate polyglot-persistence decision.

Do not introduce MongoDB into Incident Service merely for consistency
with Asset Service.

---

# 10. Technology Stack

Backend:

- Java
- Spring Boot
- Spring Security

Microservices:

- Spring Cloud Eureka
- OpenFeign
- Resilience4j

Messaging:

- Apache Kafka
- KRaft mode

Databases:

- PostgreSQL
- MongoDB

Authentication:

- JWT
- RBAC

Testing:

- JUnit 5

Observability:

- Spring Boot Actuator

DevOps:

- Docker
- Docker Compose

Frontend:

- Plain HTML/CSS/JavaScript
- nginx

Version control:

- Git
- GitHub

---

# 11. Architecture Boundaries

Each microservice owns its own database.

Do not directly access another service's database.

For example:

INCIDENT SERVICE
    |
    X
    |
ASSET DATABASE

This is forbidden.

The Incident Service must communicate with Asset Service through
its API.

---

# 12. v1 Scope

The following are intentionally included:

- Incident Service
- Asset Service
- Notification Service
- Eureka
- OpenFeign
- Resilience4j
- Kafka
- PostgreSQL
- MongoDB
- JWT
- RBAC
- SLA engine
- Incident state machine
- JUnit tests
- Docker Compose
- Actuator
- Minimal frontend

---

# 13. Explicitly Out of Scope for v1

Do NOT add these unless explicitly approved:

- API Gateway
- Redis
- Dedicated Analytics Service
- Prometheus
- Grafana
- AI/RAG classification
- GitHub Actions CI/CD
- Real email delivery
- Real SMS delivery
- Kubernetes
- Additional microservices

These may appear in the project's future roadmap.

---

# 14. Architecture Decision Rule

Do not silently change the architecture.

If an implementation requires a significant architectural change:

1. Explain the proposed change.
2. Explain why it is necessary.
3. Explain benefits.
4. Explain trade-offs.
5. Explain impact on existing services.
6. Wait for approval before implementing it.

Do not add technologies simply because they are popular.

Prefer the simplest solution that satisfies the current requirements.

---

# 15. Coding Principles

Prefer:

- Clear layered architecture
- Small focused classes
- Clear service responsibilities
- DTOs at API boundaries where appropriate
- Validation
- Centralized exception handling
- Meaningful naming
- Configuration through application configuration/environment variables
- Proper transaction boundaries
- Unit tests for business-critical logic

Avoid:

- Premature abstraction
- Overengineering
- Unnecessary design patterns
- Huge service classes
- Business logic in controllers
- Hardcoded URLs
- Hardcoded credentials
- Cross-service database access
- Duplicate business logic
- Adding dependencies without a reason

---

# 16. CLI Agent Working Rules

Before implementing a significant task:

1. Read this file.
2. Inspect the existing repository.
3. Inspect relevant existing code.
4. Understand the current implementation.
5. Identify files that need to be created or modified.
6. Provide an implementation plan.

Do not modify unrelated code.

Do not recreate existing functionality without checking first.

Do not assume the repository is empty.

Preserve working code unless there is a clear reason to change it.

After implementation:

1. Run relevant tests.
2. Check compilation.
3. Review the git diff.
4. Report created/modified files.
5. Report tests performed.
6. Report any remaining concerns.

If requirements are ambiguous, identify the ambiguity instead of making
a major architectural assumption.

---

# 17. Current Development Strategy

The project is implemented incrementally.

General flow:

Architecture
→ Design
→ CLI repository inspection
→ Implementation plan
→ Implementation
→ Tests
→ Code review
→ Git diff
→ Commit
→ Next phase

The CLI agent is responsible for implementation.

The project mentor is responsible for architecture, learning,
requirements, review and interview preparation.

The developer makes the final architectural decision.

---

# 18. Current Roadmap

Day 1:
- Define bounded contexts
- Scaffold Incident Service
- Scaffold Asset Service
- Configure PostgreSQL
- Configure MongoDB
- Basic CRUD

Day 2:
- Eureka
- OpenFeign
- Resilience4j
- Incident state machine

Day 3:
- Kafka
- IncidentCreated
- IncidentAssigned
- Notification Service

Day 4:
- SLA engine
- Scheduled breach detection
- SlaBreached event

Day 5:
- JWT
- RBAC
- Endpoint authorization

Day 6:
- Docker
- Docker Compose
- JUnit tests
- Actuator

Day 7:
- README
- Architecture diagram
- ADRs
- Demo
- Interview preparation

Day 8:
- Minimal HTML/CSS/JavaScript frontend
- nginx container

---

# 19. Important Development Principle

This project should be understandable by the developer who built it.

Do not optimize for maximum complexity.

Optimize for:

Understanding + Correctness + Practical Architecture +
Maintainability + Interview Value.

---

# 20. Current Implementation Status (added Day 7)

The sections above are the original planned architecture and roadmap.
They are preserved as history. This section records what is actually
implemented after Days 1–6. Where reality diverged from the original
plan, the actual implementation wins and the divergence is noted.

## Day-by-day completion

- Day 1-2 (complete): Incident/Asset scaffolding, PostgreSQL/MongoDB,
  CRUD, Eureka, OpenFeign, Resilience4j circuit breaker with fallback,
  incident state machine enforced in the service layer.

- Day 3 (complete): Kafka 4.0 KRaft producer/consumer; topics
  incident.created.v1 and incident.assigned.v1; AFTER_COMMIT publishing
  bridge; Notification Service (:8084, PostgreSQL) consuming events and
  exposing notification history at GET /api/v1/notifications.

- Day 4 (complete): SLA engine — priority-to-duration configuration
  (LOW PT48H / MEDIUM PT12H / HIGH PT4H / CRITICAL PT30M), slaDeadline
  persisted on creation and recalculated on priority change; scheduled
  SlaBreachScanner (30s) with slaBreachedAt dedup marker;
  incident.sla-breached.v1 topic; Notification Service consumes
  SlaBreached into SLA_BREACHED notifications.

- Day 5 (complete): JWT resource-server security (HS256 shared dev
  secret) on Incident + Asset services; roles claim mapped to
  ROLE_STUDENT / ROLE_TECHNICIAN / ROLE_MANAGER; ownership rules in the
  service layer (reporter forced from JWT subject, students own-only,
  technicians assigned-only, managers full); assignment MANAGER-only;
  Feign interceptor propagates caller JWT to Asset Service. Dev token
  mint utility instead of an auth server (intentional v1 scope).
  Notification Service intentionally left unauthenticated/internal.

- Day 6 (complete): full-stack Docker Compose — postgres, mongo,
  kafka (dual listeners: kafka:9092 internal, localhost:9092 host),
  eureka-server and all three app services from multi-stage Temurin 26
  builds; named volumes for databases; actuator health/info on all app
  services powering healthchecks; configuration externalized via
  relaxed-binding env vars; host DB ports 5433/27018 avoid native
  installations.

## Notable divergences from the original plan

- Notification Service is NOT registered with Eureka (it is purely
  event-driven plus an internal REST API). The original §2 sketch was
  neutral on this; the implementation keeps it out of discovery.

- Spring Boot 4 property naming: MongoDB connection is
  spring.mongodb.uri (env SPRING_MONGODB_URI), not
  spring.data.mongodb.uri; Eureka client env override is
  EUREKA_CLIENT_SERVICEURL_DEFAULTZONE (no spring. prefix).

- PostgreSQL enum CHECK constraints are Hibernate-generated; growing a
  Java enum requires manually widening the constraint (happened when
  SLA_BREACHED was added to NotificationEventType).

- Tests: 123 total (incident-service 84, asset-service 28,
  notification-service 11), plain JUnit/Mockito/Spring Boot Test
  against local infrastructure; no Testcontainers (deferred).

- Full architecture decision history lives in docs/adr/ (ADR-001 …
  ADR-007); README documents operations; docs/demo-script.md and
  docs/interview-prep.md support demos and interviews.