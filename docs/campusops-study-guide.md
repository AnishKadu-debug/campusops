# CampusOps Reverse-Engineering & Interview Study Guide

> **Purpose:** understand CampusOps so deeply you could rebuild it, debug it live, and defend every decision in an interview.
> **Source of truth:** this repository as of Day 7 (branch `develop`). Every class path, port, topic name, and rule mentioned here was verified against the actual code.
>
> Legend used throughout:
> - ❌ = **Not implemented in CampusOps v1** (intentionally deferred or rejected)
> - Paths are relative, e.g. `incident-service/src/main/java/com/campusops/incident/...`

---

# PART 1 — CampusOps from zero

## 1.1 What problem does CampusOps solve?

Imagine a university with hundreds of rooms: projectors die, Wi-Fi drops, AC units leak, lab equipment breaks. Students notice these problems but have no structured way to report them, track them, or hold anyone accountable for fixing them within a reasonable time.

CampusOps is an **incident management platform** for exactly this:

1. A **student** reports "the projector in Lab 7 is dead".
2. The system records it, decides how urgent it is (**priority**), and computes a **deadline** for fixing it (the **SLA**).
3. A **manager** assigns a **technician**.
4. The technician works it through its lifecycle until resolved.
5. If the deadline passes without resolution, the system itself detects the breach and generates an alert — nobody has to watch a dashboard.
6. Every meaningful step produces a **notification record** the reporter can look up.

## 1.2 What is a microservice?

A **monolith** is one application containing all features, deployed as one unit. A **microservice** architecture splits the application into small services, each:

- owning one business capability,
- owning its own data,
- running and deploying independently,
- talking to others over the network (HTTP or events).

Analogy: a restaurant with one person cooking, serving, and billing is a monolith. Splitting into kitchen, waiters, and cashier — each with their own tools and responsibilities — is microservices. Faster overall, but they now need a way to coordinate (that's where Eureka, Feign, and Kafka come in).

## 1.3 Why is CampusOps split into services?

Three reasons specific to this project:

1. **Different data shapes.** Incidents are strict and transactional; assets are flexible documents. One database can't serve both optimally (ADR-001).
2. **Different availability characteristics.** If notifications break, reporting incidents must still work. Separate services fail separately (that's the point of the circuit breaker too).
3. **Learning value.** The project deliberately exercises discovery, sync calls, async events, resilience, security, and containerization — things a monolith cannot teach.

## 1.4 The services and what each owns

| Service | Port | Owns | Database |
|---|---|---|---|
| **eureka-server** | 8761 | Service registry (the phone book) | none |
| **incident-service** | 8082 | Incidents, lifecycle state machine, priorities, SLA deadlines, breach detection, event publishing, authorization | PostgreSQL `campusops_incident` |
| **asset-service** | 8083 | Campus assets (projectors, routers, ACs…) with flexible attributes | MongoDB `campusops_asset` |
| **notification-service** | 8084 | Notification records generated from events | PostgreSQL `campusops_notification` |

Key ownership rule: **database-per-service**. Incident Service never touches Mongo; nothing reads another service's tables. Cross-service data access happens only through APIs or events.

Note: **notification-service is NOT registered with Eureka.** It is purely event-driven (consumes Kafka) plus an internal REST endpoint. Nothing discovers it by name, so it doesn't need discovery. This surprises people — remember it.

## 1.5 How the services communicate

Two styles, chosen per need:

- **Synchronous (needs an answer now):** Incident Service asks Asset Service "does asset X exist?" via OpenFeign, discovered through Eureka, protected by a Resilience4j circuit breaker.
- **Asynchronous (react when convenient):** Incident Service publishes facts to Kafka (`incident.created.v1`, `incident.assigned.v1`, `incident.sla-breached.v1`); Notification Service consumes them whenever it can.

Rule of thumb taught by this project: *use sync when the caller cannot proceed without an answer; use async when the fact matters but the reaction can happen later.*

## 1.6 Where data lives

- `campusops_incident` (PostgreSQL): table `incidents` — title, description, status, priority, reporterId, assigneeId, **sla_deadline**, **sla_breached_at**, active flag, timestamps.
- `campusops_notification` (PostgreSQL): table `notifications` — incident_id, event_type (`INCIDENT_CREATED` / `INCIDENT_ASSIGNED` / `SLA_BREACHED`), recipient_id, message, timestamps.
- `campusops_asset` (MongoDB): asset documents with name, type, location, status + free-form `attributes` map.
- Kafka: transient event log (retention-based, not queried like a DB).

## 1.7 What happens when a user creates an incident? (30-second version)

```text
Student (JWT) → POST /api/v1/incidents
  → IncidentController validates DTO
  → IncidentServiceImpl:
      - identity = JWT subject (body reporterId IGNORED)
      - role check (STUDENT only)
      - if assetId given: Feign call to Asset Service (breaker + JWT propagated)
      - compute slaDeadline from priority (SlaCalculator)
      - save row (transaction commits)
      - AFTER_COMMIT: publish IncidentCreatedEvent → Kafka
  → Notification Service consumes event → inserts notification row
  → response 201 Created with the saved incident
```

Every later flow (assign, progress, breach) follows the same skeleton.

## 1.8 Simple architecture diagram

```text
                        ┌───────────────┐
                        │    Client     │  (curl / frontend, Bearer JWT)
                        └───────┬───────┘
                                │ HTTPS
                     ┌──────────▼──────────┐        register ▲
                     │ Incident Service    │◄───────────────┤
                     │ :8082               │                │
                     └──┬────────┬─────────┘         ┌──────┴──────┐
             incidents   │        │ Feign+JWT          │   Eureka   │
             (owner)     │        │ +circuit breaker   │  :8761     │
                    ┌────▼───┐    └──────────────►┌─────▼────────┐◄──┘
                    │Postgres│                   │Asset Service │
                    │incident│                   │ :8083        │
                    └────────┘                   └─────┬────────┘
                          │                            │
                          │ events (after commit)      │ assets (owner)
                          ▼                       ┌────▼─────┐
                    ┌───────────┐                 │ MongoDB  │
                    │   Kafka   │                 └──────────┘
                    └─────┬─────┘
                          │ consume
                   ┌──────▼────────┐
                   │ Notification  │
                   │ Service :8084 │
                   └──────┬────────┘
                          │
                    ┌─────▼──────┐
                    │ Postgres   │
                    │notification│
                    └────────────┘
```

---

# PART 2 — Technology stack (what actually exists)

Every row below exists in the repository today. Nothing speculative.

| Technology | What it is | Why CampusOps uses it | Where it lives |
|---|---|---|---|
| Java 26 | Language/runtime | Project compiles with `release 26`; Docker images built on Temurin 26 | All four `pom.xml` files |
| Spring Boot 4.1.1 | Opinionated Spring runtime + autoconfiguration | Removes boilerplate; starters per capability | Parent of all poms |
| Maven (per-service wrappers) | Build tool + dependency management | Four independent projects, each with own `mvnw` | `*/mvnw`, `*/pom.xml` |
| REST over HTTP | API style between clients and services | Public surface of every app service | Controllers |
| Spring MVC (`spring-boot-starter-webmvc`) | HTTP request handling | Serves JSON endpoints | All three app services |
| Bean Validation (jakarta) | Declarative input checks (`@NotBlank`, `@Size`) | Reject bad requests early with 400s | Request DTOs |
| Spring Data JPA + Hibernate | ORM + repository abstraction over PostgreSQL | Type-safe persistence without SQL strings | incident + notification services |
| PostgreSQL 18 | Relational database | Transactional lifecycle + notification data | Compose `postgres`, native dev instance |
| MongoDB 7 | Document store | Flexible asset attributes | Compose `mongo`, native dev instance |
| Spring Cloud 2025.1.2 | Microservices toolkit on Boot 4 | Brings Eureka/Feign starters | dependencyManagement in poms |
| Eureka (Netflix) | Service registry | Name-based discovery instead of URLs | `eureka-server`, registered clients |
| OpenFeign | Declarative HTTP client | `AssetClient` interface → real HTTP calls | `incident-service/.../client/` |
| Resilience4j | Circuit breaker | Contains Asset outages; fallback keeps creation working | `AssetServiceClient`, `application.properties` |
| Apache Kafka 4.0 (KRaft) | Event log/broker | Async facts to Notification Service | Compose `kafka`, producer+consumer |
| Spring Kafka | Spring integration for Kafka | `KafkaTemplate`, `@KafkaListener` | incident (producer), notification (consumer) |
| Spring Security | Filter chain, authn/authz | Secures both API services | `security/SecurityConfig` ×2 |
| OAuth2 Resource Server (Nimbus) | JWT validation machinery | Decodes/validates HS256 tokens | Both resource servers |
| JWT (JSON Web Token) | Signed identity token format | Caller identity + roles | minted by DevTokenGenerator |
| RBAC | Role-based access control | STUDENT/TECHNICIAN/MANAGER rules | Controller annotations + service layer |
| Spring application events | In-JVM pub/sub | Bridge between transactions and Kafka | `IncidentEventPublisher` |
| `@TransactionalEventListener(AFTER_COMMIT)` | Run logic only after commit | Guarantees events describe committed facts | Same class |
| Spring Scheduling | In-process scheduled jobs | SLA breach scanner | `scheduler/SlaBreachScanner.java` |
| Docker + Compose | Containerization/orchestration-lite | Full stack from one command | 4× `Dockerfile`, root `docker-compose.yml` |
| Multi-stage builds | Slim images built from clean checkout | Builder stage compiles; runtime stage runs JRE-only | Every `Dockerfile` |
| Actuator | Production endpoints (health/info) | Healthchecks gate Compose startup | All three app services |
| JUnit 5 | Test framework | 123 tests total | All services' test trees |
| Mockito | Mocking for unit tests | Isolate service logic from repos/clients | `IncidentServiceTest` et al. |
| MockMvc | Servlet-layer testing | Full-stack security tests with real filter chain | `api/IncidentApiSecurityTest` |
| Git/GitHub | Version control | Branch `develop`; Days committed per feature | `.git` |

Explicitly **not present** (❌ say this in interviews): API gateway, Redis, Kubernetes, Prometheus/Grafana/tracing, Flyway/Liquibase, Testcontainers, auth server/login/password store, refresh tokens, RS256, email/SMS delivery, frontend beyond roadmap plans.

---

# PART 3 — Java + Spring Boot foundation

## 3.1 What is Spring Boot?

Plain Spring historically needed mountains of XML/manual wiring. **Spring Boot** = Spring + autoconfiguration + embedded server + opinionated starters. You add `spring-boot-starter-webmvc` and a working HTTP server appears; add `spring-boot-starter-data-jpa` and a connection pool + JPA stack configure themselves.

**In CampusOps:** every service is a Boot app. Entry points:
- `incident-service/.../IncidentServiceApplication.java` — `@SpringBootApplication`, plus `@EnableFeignClients` and `@EnableScheduling` (each enables one capability).
- `asset-service/.../AssetServiceApplication.java`
- `notification-service/.../NotificationServiceApplication.java`
- `eureka-server/.../EurekaServerApplication` with `@EnableEurekaServer`.

Boot 4.1.1 parent in every `pom.xml`; Java 26.

**Interview:** *"What does @SpringBootApplication actually do?"* → It's `@Configuration` + `@ComponentScan` (scan this package tree for beans) + `@EnableAutoConfiguration` (configure beans based on what's on the classpath).

## 3.2 Dependency Injection & IoC

**IoC (Inversion of Control):** you don't construct your dependencies; the framework creates them and hands them to you.
**Dependency Injection (DI):** the mechanism — dependencies arrive via constructor parameters.

```java
// incident-service/.../service/impl/IncidentServiceImpl.java
@Service
@RequiredArgsConstructor                      // Lombok generates the constructor
public class IncidentServiceImpl implements IncidentService {
    private final IncidentRepository incidentRepository;
    private final AssetServiceClient assetServiceClient;
    private final ApplicationEventPublisher eventPublisher;
    private final SlaCalculator slaCalculator;
```

Nobody calls `new IncidentServiceImpl(...)`. Spring instantiates it, sees it needs those four collaborators, finds beans of those types, injects them.

**Why CampusOps needs this:** swapping/stubbing becomes trivial — tests pass mocks into the same constructor (`IncidentServiceTest` uses Mockito `@InjectMocks` exactly this way). Also single-instance guarantees without manual singletons.

## 3.3 Beans

A **bean** = an object whose lifecycle Spring manages (create once, wire, keep in the *application context*). Anything annotated `@Component` (or meta-annotations) or declared in `@Bean` methods becomes one.

CampusOps examples beyond components:
- `SecurityConfig#jwtDecoder()` — a `@Bean JwtDecoder` built from config properties.
- `KafkaTopicConfig` declares three `NewTopic` beans so the broker auto-creates topics at startup.

## 3.4 Stereotype annotations

| Annotation | Meaning | CampusOps example |
|---|---|---|
| `@Component` | generic managed bean | `FeignAuthPropagationInterceptor`, `CurrentUserResolver` |
| `@Service` | business-logic layer bean | `IncidentServiceImpl`, `NotificationServiceImpl`, `SlaCalculator` |
| `@Repository` | persistence-layer bean (+ exception translation) | `IncidentRepository` (interface — Spring Data implements it), `AssetRepository` |
| `@RestController` | HTTP endpoint bean; returns data, not views | `IncidentController`, `AssetController`, `NotificationController` |
| `@Configuration` | class producing `@Bean`s | `SecurityConfig`, `KafkaTopicConfig`, test `KafkaTestConfig` |

## 3.5 Layering: Controller → Service → Repository

- **Controller layer:** translate HTTP ↔ Java. No business rules. (`IncidentController`)
- **Service layer:** business rules, transactions, event publishing. (`IncidentServiceImpl`) — all authorization ownership checks live here deliberately (ADR-005).
- **Repository/persistence:** database access only. (`IncidentRepository`)
- **DTOs:** shapes crossing the network boundary, kept separate from entities (`dto/request/*`, `dto/response/*` vs `entity/*`).

Why separate DTOs from entities? Entities carry DB concerns (columns, generated IDs); DTOs expose exactly what clients should send/receive and stay stable while internals evolve. `IncidentResponse.fromEntity(entity)` does the mapping explicitly.

## 3.6 Configuration classes & application.properties

`application.properties` per service holds ports, datasource URLs, Kafka settings, Eureka URL, SLA durations, JWT secret, actuator exposure. Boot loads it automatically; environment variables override any line via relaxed binding (that's how Docker reconfigures everything without touching files — see Part 16).

Two real naming lessons recorded here because interviews love war stories:
- Mongo URI property in Boot 4.1 is `spring.mongodb.uri` → env `SPRING_MONGODB_URI` (**not** `SPRING_DATA_MONGODB_URI`).
- Eureka client URL is `eureka.client.service-url.defaultZone` → env `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` (**no `spring.` prefix exists on that family** — using `SPRING_EUREKA_…` binds to a nonexistent key and silently does nothing; we hit this live in Day 6).

**Common interview questions (Part 3):**
1. Difference between `@Component`, `@Service`, `@Repository`? (Semantic layers; `@Repository` adds exception translation.)
2. Constructor vs field injection? (Constructor makes dependencies explicit and testable — CampusOps uses constructor injection everywhere via Lombok `@RequiredArgsConstructor`.)
3. What is the application context? (The container holding beans and their wiring.)
4. Where does configuration come from, in order? (Defaults < application.properties < environment variables < command-line args.)

---

# PART 4 — REST API

## 4.1 The absolute basics

- **HTTP methods used by CampusOps:** `GET` (read), `POST` (create), `PUT` (full update), `PATCH` (partial/targeted change like status or assignment). `DELETE` exists only on assets.
- **Status codes actually returned by CampusOps:** 200 OK, 201 Created, 400 Bad Request (validation / illegal state transition), 401 Unauthorized (missing/invalid JWT), 403 Forbidden (authenticated but not allowed), 404 Not Found, 500 Internal Server Error (unexpected).
- **Path variable** identifies *which* resource: `/api/v1/incidents/{id}`.
- **Query parameter** filters a collection: `/api/v1/incidents?status=OPEN`.
- **JSON request body** carries payload, mapped onto a DTO.

## 4.2 How CampusOps exposes its APIs

Real endpoints:

```text
incident-service :8082   /api/v1/incidents
   POST /                create (STUDENT; reporter forced from JWT)
   GET  /?status=        role-scoped list
   GET  /{id}            ownership-checked detail
   PUT  /{id}            update details
   PATCH /{id}/status    lifecycle transition
   PATCH /{id}/assign    MANAGER only

asset-service :8083      /api/v1/assets
   POST /  GET /?type=&status=  GET /{id}  PUT /{id}  DELETE /{id}

notification-service :8084   /api/v1/notifications
   GET  /?incidentId=    history (unauthenticated internal v1 ❌ auth)
```

Example controller slice (`IncidentController`):

```java
@PostMapping
public ResponseEntity<IncidentResponse> createIncident(
        @Valid @RequestBody CreateIncidentRequest request,
        Authentication authentication) { ... }
```

`@Valid` triggers Bean Validation on the DTO (`@NotBlank title`, `@NotNull priority`, `@Size(max=150)`…). Failures raise `MethodArgumentNotValidException`.

## 4.3 Request lifecycle (the skeleton every flow follows)

```text
Client → SecurityFilterChain (JWT filter validates token)
       → Controller (DTO binding + validation + CurrentUser resolution)
       → Service (business rules, ownership checks, transaction)
       → Repository / Feign client
       → PostgreSQL / MongoDB / Asset Service
       → entity saved → mapped to Response DTO
       ← JSON back to client
```

## 4.4 Validation & centralized exception handling

One class handles everything consistently: `exception/GlobalExceptionHandler.java` (`@RestControllerAdvice`):

| Exception | Result |
|---|---|
| `ResourceNotFoundException` | 404 + ErrorResponse |
| `ForbiddenException` (ours) | 403 |
| `AccessDeniedException` (Spring's, from `@PreAuthorize`) | 403 (handler added specifically so method-security denials don't fall into the catch-all 500) |
| `MethodArgumentNotValidException` | 400 + field→message map |
| `IllegalArgumentException`, `IllegalStateException` | 400 (e.g. "Invalid status transition from OPEN to CLOSED") |
| `Exception` | 500 catch-all |

Every response uses the same `ErrorResponse` shape: timestamp, status, error, message, path (+ validationErrors when present). The security filter chain produces the same shape for 401/403 before reaching MVC.

**Why centralize?** Controllers stay clean; error format is uniform; adding a new exception type is one handler method.

**Common interview questions (Part 4):**
1. PUT vs PATCH? (PUT replaces full state — our update requires title/description/priority; PATCH applies a targeted change — status, assignment.)
2. Why return 201 on create? And why include the created resource body? (Client gets generated id + computed fields like slaDeadline immediately.)
3. What does `@RestController` add over `@Controller`? (Implicit `@ResponseBody` — return values serialize to JSON.)
4. 401 vs 403? (401 = who are you? failed. 403 = I know you, but no.) — CampusOps enforces this split deliberately.
5. How do you handle exceptions across many controllers? (Single `@RestControllerAdvice`.)

---

# PART 5 — Databases + persistence

## 5.1 PostgreSQL in simple terms

A **relational database** stores data in **tables** (rows × columns) with strict types. A **primary key** uniquely identifies each row (`incidents.id`, auto-generated `IDENTITY`). **Constraints** enforce rules at the DB level (`NOT NULL`, max lengths, and — importantly here — Hibernate-generated CHECK constraints on enum columns). **Foreign keys** would reference other tables; CampusOps keeps references as plain string IDs (`reporterId`, `assigneeId`, `assetId`) because those entities live in *other services'* databases.

## 5.2 Transactions & ACID

A **transaction** groups database operations into all-or-nothing units.
**ACID:** Atomicity (all or nothing), Consistency (rules hold before and after), Isolation (concurrent transactions don't trample each other), Durability (committed = survives crash).

CampusOps transaction boundaries: class-level `@Transactional(readOnly = true)` on `IncidentServiceImpl`, with `@Transactional` (read-write) on each mutating method. Same pattern in `NotificationServiceImpl`. The SLA scanner's scan method is one transaction too — marking breaches and publishing events commit together.

## 5.3 JPA, Hibernate, Spring Data JPA — three different things

- **JPA** = the specification (annotations + interfaces: "map this class to a table").
- **Hibernate** = the implementation engine that actually generates SQL (version 7.4.x here).
- **Spring Data JPA** = Spring layer on top: you write interface methods, it derives queries.

Entity example — `incident-service/.../entity/Incident.java`:

```java
@Entity @Table(name = "incidents")
public class Incident extends BaseAuditEntity {   // createdAt/updatedAt via Hibernate @CreationTimestamp/@UpdateTimestamp
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    ...
    @Enumerated(EnumType.STRING)                 // store "OPEN", not ordinal 0
    private IncidentStatus status;
    @Column(name = "sla_deadline")
    private Instant slaDeadline;
    @Column(name = "sla_breached_at")
    private Instant slaBreachedAt;
```

Always use `EnumType.STRING` — ordinals silently break when you reorder enum values.

## 5.4 Repository abstraction

```java
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByActiveTrueAndStatusInAndSlaDeadlineBeforeAndSlaBreachedAtIsNull(
            Collection<IncidentStatus> statuses, Instant deadline);
}
```

Spring Data parses the method name into SQL. No implementation class exists. The long breach-finder name reads like the query it produces — that's idiomatic derived-query style.

**Repository inventory (real):**
- Incident: `findByActiveTrue`, `findByActiveTrueAndStatus`, `findByIdAndActiveTrue`, `findByReporterIdAndActiveTrue`, `findByAssigneeIdAndActiveTrue`, plus the breach finder above. Day 5 needed **zero new queries** — ownership scoping reused existing finders.
- Notification: `findByIncidentIdOrderByCreatedAtDesc`, `findAllByOrderByCreatedAtDesc`.

## 5.5 ddl-auto and the CHECK-constraint lesson

`spring.jpa.hibernate.ddl-auto=update` makes Hibernate create missing tables/columns at startup. It never alters existing constraints.

Live lesson from Day 4: Hibernate 7 also generates implicit **CHECK constraints** for `@Enumerated(STRING)` columns. When `SLA_BREACHED` was added to `NotificationEventType`, inserts failed with SQLState 23514 because the DB constraint still listed only the two original values. Fix was a manual, data-preserving:

```sql
ALTER TABLE notifications
  DROP CONSTRAINT notifications_event_type_check,
  ADD CONSTRAINT notifications_event_type_check
  CHECK (event_type IN ('INCIDENT_CREATED','INCIDENT_ASSIGNED','SLA_BREACHED'));
```

Moral: schema-by-Hibernate is fine for v1 but has sharp edges — migration tooling (Flyway/Liquibase) is ❌ not implemented and is the documented future fix.

## 5.6 Why PostgreSQL for Incident AND Notification?

Both domains are relational: incidents need ACID transitions and deadline columns; notifications are small structured rows queried by incident id. Simple, correct fit.

*"Why not MongoDB everywhere?"* → We'd lose constraints/transactions guarantees around state transitions and write Mongo queries for naturally tabular data. *"Why not Postgres everywhere?"* → Asset attributes differ per type (projector: resolution/lampHours; router: ip/firmware/ports); documents avoid sparse-column schemas. This is the polyglot persistence decision (ADR-001).

## 5.7 MongoDB for assets

A **document database** stores JSON-like **documents** in **collections** with flexible schema — every document can have different fields.

Asset document shape (from `CreateAssetRequest` / entity): `name`, `type` (PROJECTOR/ROUTER/…), `location`, optional `status`, and an open **`attributes` map** where type-specific fields live. Spring Data MongoDB gives the same repository experience (`AssetRepository extends JpaRepository`-style CRUD via Mongo templating).

Connection property note (Boot 4.1): `spring.mongodb.uri=mongodb://localhost:27017/campusops_asset`.

**Common interview questions (Part 5):**
1. JPA vs Hibernate vs Spring Data JPA? (Spec vs engine vs Spring convenience layer.)
2. What does `@Transactional` actually do? (Opens/closes a transaction around the method; rollback on unchecked exceptions by default.)
3. Why STRING enums? (Ordinals break on reorder.)
4. What happened when you added an enum value? (CHECK constraint story — tell it as a debugging war story.)
5. When would you choose documents over tables? (Heterogeneous, evolving per-entity shapes — exactly the asset case.)

---

# PART 6 — Microservices

## 6.1 Monolith vs microservices

| | Monolith | Microservices |
|---|---|---|
| Deployment | one unit | many units |
| Database | usually shared | one per service |
| Failure blast radius | whole app | single service |
| Network calls | none internally | constant |
| Operational cost | low | high |

Microservices trade operational simplicity for independent scaling/deployment/failure isolation. At CampusOps' scale it's a learning architecture — say so honestly.

## 6.2 Service boundaries in CampusOps

Boundaries follow business capability:
- **Incident Service** — the workflow core: lifecycle, priorities, SLAs, breach detection, authorization rules, event publishing. Owns `campusops_incident`.
- **Asset Service** — equipment master data with flexible attributes. Owns `campusops_asset`.
- **Notification Service** — pure event consumer + history API. Owns `campusops_notification`.
- **Eureka Server** — infrastructure service (the registry), no business data.

Rule enforced everywhere: a service may read another service's data only through its API or its events — never its tables. That's why `incidentId` inside a notification row is a copied value, not a foreign key across services.

## 6.3 Why notification-service skips Eureka

Discovery exists so callers can find you. Nothing calls Notification Service by name — it subscribes to Kafka topics and exposes one internal GET endpoint. No callers → no registry entry needed. Fewer moving parts, honest design.

## 6.4 Advantages & costs (say both in interviews)

Advantages realized: independent failure isolation (Kafka down ≠ reporting down), per-domain databases, clear team-sized codebases, independent restarts during development.
Costs paid: network latency and failure modes between services, duplicated event DTOs, distributed debugging (logs in multiple containers), infra to run (registry + broker + two DB engines).

**Common interview questions (Part 6):**
1. How did you choose service boundaries? (Business capabilities + distinct data shapes, then verified no shared tables.)
2. What is database-per-service and why? (Ownership + independence; enforced by copying ids instead of cross-service FKs.)
3. When would you NOT use microservices? (Small team, unclear boundaries, early product — say CampusOps is deliberately educational.)
4. How do services stay consistent without shared transactions? (They don't share transactions — they accept eventual consistency via events; see Part 11.)

---

# PART 7 — Eureka service discovery

## 7.1 The problem it solves

Incident Service needs to call Asset Service. Without discovery you hardcode `http://localhost:8083` — which breaks the moment the port changes, a second instance appears, or everything moves into containers where "localhost" means *the wrong machine*.

**With Eureka:** services register themselves under a logical name; callers ask the registry for instances of that name.

```text
Without: Incident → http://localhost:8083/api/v1/assets/…      (brittle)
With:    Incident → "asset-service" → ask Eureka → real instance address
```

## 7.2 The moving parts

- **Eureka Server:** the registry. CampusOps runs one at :8761 (`eureka-server` module, `@EnableEurekaServer`, client registration disabled on itself).
- **Eureka Client:** any service that registers + fetches the registry (incident-service, asset-service). Configured in each `application.properties`: `eureka.client.service-url.defaultZone=http://localhost:8761/eureka/`, `prefer-ip-address=true`.
- **Registration & heartbeat:** clients POST their location and send heartbeats; missing heartbeats expire entries.
- **Discovery:** Feign asks the local registry cache for an instance of `asset-service` and load-balances across them.

## 7.3 The Docker lesson (tell this story)

Inside Compose, each container has its own network namespace — `localhost` inside the incident container is **the incident container**, not the host and not other containers. Services must use Compose service names: `postgres:5432`, `mongo:27017`, `kafka:9092`, `eureka-server:8761`.

The live bug we hit: we set env var `SPRING_EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/`. It reached the container fine but bound to nothing, because the property family has **no `spring.` prefix**: the real key is `eureka.client.service-url.defaultZone`. Correct variable:

```text
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
```

Symptom was `Connection refused … http://localhost:8761/eureka/` in `docker logs campusops-incident`; fix verified via registration logs (`registration status: 204`) and zero localhost errors afterwards.

## 7.4 Who registers?

| Service | Registers? | Why |
|---|---|---|
| incident-service | ✅ | Calls asset-service by name |
| asset-service | ✅ | Is called by name |
| notification-service | ❌ | Nobody calls it by name; pure Kafka consumer |
| eureka-server | n/a | It IS the registry |

**Common interview questions (Part 7):**
1. What problem does service discovery solve? (Dynamic locations; remove hardcoded endpoints.)
2. How do stale instances disappear? (Heartbeat lease expiry.)
3. Client-side vs server-side discovery? (CampusOps = client-side: Feign reads the registry itself; contrast with a load-balancer-in-the-middle.)
4. What happens when Eureka is down? (Clients keep operating from their cached registry; new registrations fail — see Part 20.)
5. Why doesn't notification-service register? (No inbound name-based callers.)

---

# PART 8 — OpenFeign

## 8.1 What it is

OpenFeign turns an interface into an HTTP client. You declare methods with annotations; Feign generates the calls, integrates with Eureka load balancing, and applies interceptors.

## 8.2 CampusOps' actual client chain

```java
// client/AssetClient.java
@FeignClient(name = "asset-service", path = "/api/v1/assets")
public interface AssetClient {
    AssetResponseDto getAssetById(@PathVariable String id);
}
```

`name = "asset-service"` is the **logical service name** — resolved through Eureka, not DNS. A hand-written wrapper adds resilience:

```java
// client/AssetServiceClient.java
@CircuitBreaker(name = "assetService", fallbackMethod = "getAssetFallback")
public Optional<AssetResponseDto> getAssetById(String assetId) { ... }   // also maps FeignException.NotFound -> empty

public Optional<AssetResponseDto> getAssetFallback(String assetId, Throwable t) {
    return Optional.of(placeholder "UNAVAILABLE (Circuit Breaker Fallback)");
}
```

Note the deliberate detail: **404 is not a failure** — a missing asset returns empty normally; only connection/timeout-type failures should trip the breaker.

## 8.3 Why asset validation is synchronous

While creating an incident, the caller needs the answer *now*: reject the request if the asset doesn't exist. There's no meaningful async version — "validate later" would mean accepting incidents pointing at nothing. Sync dependency + async events is exactly the communication rule from Part 1.

If you pass no `assetId`, the Feign path never executes — creating without assets works even with Asset Service completely down.

**Common interview questions (Part 8):**
1. How does Feign find the service? (Logical name → Eureka registry → load-balanced instance.)
2. How does a declarative client work? (Interface proxy generating HTTP requests from annotations.)
3. Where would you add headers to every Feign call? (A `RequestInterceptor` bean — CampusOps uses one for JWT propagation, Part 15.)

---

# PART 9 — Resilience4j circuit breaker

## 9.1 Failure propagation — the problem

If Asset Service hangs, Incident Service threads block waiting, requests pile up, Incident Service exhausts its thread pool too — one slow dependency takes down healthy services. That's **cascading failure**.

## 9.2 The circuit breaker, in three states

Analogy: an electrical breaker that trips so a fault doesn't burn the house.

- **CLOSED** (normal): calls flow through; failures are counted.
- **OPEN** (tripped): calls fail *immediately* without touching the network; fallback answers instead. After a wait period…
- **HALF_OPEN**: a limited number of test calls are allowed through; success closes the circuit, failure reopens it.

## 9.3 CampusOps' exact configuration

From `incident-service/src/main/resources/application.properties`, instance `assetService`:

```properties
sliding-window-type=COUNT_BASED        # judge by last N calls
sliding-window-size=5                  # look at last 5 calls
minimum-number-of-calls=3              # need ≥3 before judging
failure-rate-threshold=50              # trip at ≥50% failures
wait-duration-in-open-state=5s         # stay open 5s
permitted-number-of-calls-in-half-open-state=2   # 2 probes in half-open
automatic-transition-from-open-to-half-open-enabled=true
```

Read as: "after at least 3 recent calls, if half of the last 5 failed, stop calling Asset Service for 5 seconds; then let 2 probe calls decide."

## 9.4 The fallback

`getAssetFallback(assetId, throwable)` returns a placeholder asset marked `"UNAVAILABLE (Circuit Breaker Fallback)"`, so incident creation still succeeds with degraded information. Design choice: availability over strictness for this optional reference. Note what the fallback is NOT: it doesn't silently pretend validation succeeded — the placeholder name makes degradation visible in data.

## 9.5 Why not Kafka for asset validation?

Because the caller requires the answer during the request ("does this asset exist?" gates creation). Kafka is for facts others react to later. Choosing sync+breaker here and async there *is* the architecture lesson. *(ADR-002 vs ADR-003)*

**Common interview questions (Part 9):**
1. Explain CLOSED/OPEN/HALF_OPEN. (Above — draw it.)
2. Circuit breaker vs retry? (Retry fights transient errors; the breaker stops hammering a failing dependency. They compose.)
3. What triggers OPEN in CampusOps numbers? (≥3 calls made, ≥50% of last 5 failed.)
4. What does your fallback return and why? (Visible placeholder — availability without silent lies.)

---

# PART 10 — Kafka

## 10.1 From zero

- **Message broker:** a service that receives messages and delivers them to interested parties, decoupling senders from receivers in time and availability.
- **Producer / consumer:** writer and reader of messages.
- **Topic:** a named, append-only log for one kind of message. CampusOps topics:
  - `incident.created.v1`
  - `incident.assigned.v1`
  - `incident.sla-breached.v1`
- **Partition:** a topic is split into partitions for parallelism; order is guaranteed *within* a partition only.
- **Offset:** a consumer's position within a partition (like a bookmark).
- **Consumer group:** a named set of consumers sharing the work — each partition goes to exactly one member of the group. CampusOps group: `notification-service`.
- **Asynchronous communication:** producer doesn't wait; consumers react later. If the consumer is down, messages wait in the log.

## 10.2 CampusOps' flow

```text
IncidentService (producer)                    NotificationService (consumer)
KafkaTemplate.send(topic, key=incidentId, event)   @KafkaListener methods
        └──────────────► incident.created.v1 ──────────┤ onIncidentCreated
                        incident.assigned.v1 ──────────┤ onIncidentAssigned
                        incident.sla-breached.v1 ──────┤ onSlaBreached
```

Serialization details worth knowing (`application.properties` both sides):
- Producer: `JsonSerializer`, `acks=all` (broker acks before success), **type headers disabled**.
- Consumer: `ErrorHandlingDeserializer` wrapping Jackson, `spring.json.use.type.headers=false`, each listener pins its DTO via `spring.json.value.default.type=…`. Services never share event classes — small duplicated DTOs keep coupling zero.

## 10.3 Why asynchronous? Why not call Notification Service directly?

The reporter doesn't need the notification inside the HTTP response. Calling Notification directly would: couple uptime (notification outage breaks incident creation), add latency to every create/assign/breach, and force Incident Service to know notification internals. Events invert that: publish a fact, forget about it.

## 10.4 KRaft — why there's no ZooKeeper

Historically Kafka needed ZooKeeper to track broker metadata/coordination. **KRaft** (Kafka Raft) moves that metadata into Kafka itself via an internal Raft protocol controller quorum. CampusOps runs a single combined broker+controller node:

```text
KAFKA_PROCESS_ROLES=broker,controller      KAFKA_NODE_ID=1
CONTROLLER listener localhost:29093, quorum voter 1@localhost:29093
```

One less system to run — and a good "why?" answer.

## 10.5 Dual listeners (containers vs host)

One broker, two audiences:

```text
PLAINTEXT       kafka:9092    ← containers talk here (advertised as kafka:9092)
PLAINTEXT_HOST  :9094         ← host tools reach it via mapped port localhost:9092
```

Advertised addresses matter: the broker tells clients where to connect, so the container audience must hear `kafka:9092` while host tooling hears `localhost:9092`.

## 10.6 Delivery semantics

CampusOps is **at-least-once**: producer retries with `acks=all`; consumers commit offsets after processing. Duplicates are possible (crash between processing and offset commit). v1 tolerates this because notifications are simple inserts; a stricter design would add idempotency keys per event. Say this honestly if asked "exactly-once?"

**Message keys & ordering:** every event is keyed by incident id → all events for one incident land in the same partition → they're consumed in order for that incident. Different incidents parallelize across partitions.

**Common interview questions (Part 10):**
1. Topic vs queue? (Log with named consumers/groups vs point-to-point removal.)
2. What's an offset/consumer group? (Above — use the bookmark/team analogy.)
3. How do you get ordering? (Key by entity id; order guaranteed per partition.)
4. At-least-once vs at-most-once vs exactly-once? (Where CampusOps sits and why it's acceptable.)
5. Why no ZooKeeper? (KRaft.)

---

# PART 11 — Transactions + AFTER_COMMIT ⭐

This is the most interview-dense mechanism in the project. Learn it cold.

## 11.1 The danger being avoided

Imagine publishing to Kafka *before* or *during* the database work:

```text
save incident → publish IncidentCreatedEvent → transaction FAILS → rollback
                                ↓
              Notification Service already stored
        "Your incident has been registered" … for an incident
              THAT DOES NOT EXIST ANYWHERE.
```

A phantom fact left the system. Consumers can't tell it apart from real ones.

## 11.2 The CampusOps solution, step by step

```java
// IncidentServiceImpl.createIncident — inside @Transactional
incidentRepository.save(incident);                       // not committed yet!
eventPublisher.publishEvent(new IncidentCreatedEvent…); // NOT Kafka yet — in-JVM event

// IncidentEventPublisher
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onIncidentCreated(IncidentCreatedEvent e) {
    kafkaTemplate.send(incidentCreatedTopic, e.getIncidentId().toString(), e);
}
```

Mechanics:
1. `publishEvent` registers an in-JVM Spring event — nothing leaves the process.
2. The transaction continues and commits.
3. **Only after successful commit** does Spring invoke the AFTER_COMMIT listener, which then talks to Kafka.

So the ordering guarantee becomes: *database row exists ⇒ event may exist*; never the reverse.

## 11.3 The SLA scanner uses the same trick

`SlaBreachScanner.scanForBreachedIncidents()` is itself `@Transactional`: it marks `slaBreachedAt` on each found incident AND publishes `SlaBreachedEvent`s inside the transaction. Commit succeeds → AFTER_COMMIT fires → Kafka gets events describing rows that definitely persisted. Rollback → nothing published.

Subtlety worth mentioning in interviews: `@TransactionalEventListener(AFTER_COMMIT)` silently does nothing when there is no active transaction (default `fallbackExecution=false`). That's why the scanner method is annotated `@Transactional` even though its repository writes could survive without it.

## 11.4 Remaining gap (honesty point)

AFTER_COMMIT closes *rollback* phantom events. A crash *between* commit and Kafka send still loses the event — that's the known cost of this pattern; full coverage needs outbox/CDC patterns, which are ❌ not implemented in v1. Saying that unprompted scores points.

**Common interview questions (Part 11):**
1. Why is publishing before commit dangerous? (Phantom-event story above.)
2. What does `@TransactionalEventListener(AFTER_COMMIT)` do? Where is it in CampusOps?
3. What happens on rollback? (Listener never invoked.)
4. What's still possible after commit? (Send failure/crash loss → outbox pattern as future fix.)

---

# PART 12 — SLA engine + scheduling

## 12.1 Concepts

- **SLA (Service Level Agreement):** the promised resolution window. Here: priority → duration.
- **Deadline:** the concrete timestamp promise ends (`sla_deadline`, `Instant`).
- **Scheduled job / polling scan:** code that runs periodically and sweeps for work, instead of reacting instantly to each event.

| Priority | Duration | Source |
|---|---|---|
| LOW | PT48H | `campusops.sla.duration.low` |
| MEDIUM | PT12H | `campusops.sla.duration.medium` |
| HIGH | PT4H | `campusops.sla.duration.high` |
| CRITICAL | PT30M | `campusops.sla.duration.critical` |

All ISO-8601 durations; overridable by env (`CAMPUSOPS_SLA_DURATION_CRITICAL=PT1M` is how demos breach quickly).

## 12.2 Deadline creation

`service/SlaCalculator.java`: maps enum → `Duration`, `calculateDeadline(priority)` = `Instant.now() + duration`.
Called in `createIncident` (initial deadline) and in `updateIncident` **only when priority changed** — raising urgency re-arms a shorter clock; lowering extends it. Changing title/description never touches the deadline.

## 12.3 The scanner

`scheduler/SlaBreachScanner.java`:

```java
@Scheduled(fixedDelayString = "${campusops.sla.scan-interval}")   // PT30S default
@Transactional
public void scanForBreachedIncidents() { ... }
```

Each run:
1. Query: active + status ∈ {OPEN, ASSIGNED, IN_PROGRESS} + `slaDeadline < now` + `slaBreachedAt IS NULL` (one derived finder).
2. For each hit: set `slaBreachedAt = now`, publish `SlaBreachedEvent` (fields: eventId, occurredAt, incidentId, title, priority, reporterId, assigneeId, slaDeadline, breachedAt).
3. Transaction commits → AFTER_COMMIT → Kafka `incident.sla-breached.v1` → Notification Service stores `SLA_BREACHED` row addressed to the reporter.

`fixedDelay` = gap measured from the END of the previous run (no overlapping scans).

## 12.4 Why slaBreachedAt exists (idempotency/dedup)

Without the marker, every scan would re-fire breaches forever. With it, the query itself filters out processed incidents → **exactly one breach event per incident, guaranteed by database state**, surviving restarts and any number of scans. This is application-level idempotency: make the operation safe to repeat.

## 12.5 Why RESOLVED/CLOSED are excluded

An SLA measures time-to-resolution. Once resolved/closed, the promise's outcome is settled — flagging a breach afterwards would notify about work already finished. Status filtering lives in the finder, so closed incidents can't slip through.

Live proof from Day 6 verification: CRITICAL incident created 12:46:41Z with PT1M window → SLA_BREACHED notification stamped 12:47:50Z (~69s), exactly one row despite multiple scans passing through that window.

**Common interview questions (Part 12):**
1. Polling vs event-driven detection — trade-offs? (Simple/stateful-safe vs instant-but-complex; v1 picks polling deliberately, latency ≤ interval.)
2. How do you prevent duplicate breach events? (Marker column + filtered query = state-driven dedup.)
3. fixedRate vs fixedDelay? (Wall-clock cadence vs end-of-run cadence; overlap safety.)
4. What happens to the deadline if priority changes? (Recalculated only on change — explain both directions.)

---

# PART 13 — Spring Security + JWT

## 13.1 Concepts from zero

- **Authentication:** proving *who* you are. **Authorization:** deciding *what you may do*. CampusOps strictly separates them.
- **Identity:** the authenticated principal — in CampusOps, always the JWT `sub` claim.
- **Role / permission:** roles are coarse groups (STUDENT, TECHNICIAN, MANAGER); permissions are what actions those roles may take.
- **JWT (JSON Web Token):** `header.payload.signature`, base64url segments. Header names the algorithm; payload holds **claims** (`sub`, `roles`, `exp`, `iat`, `jti`); the **signature** is an HMAC of the first two segments using a secret — tampering with either segment invalidates it. Tokens are *verified*, not looked up: the service needs no session store.

## 13.2 The CampusOps request path

```text
Client: Authorization: Bearer eyJhbGciOi…
  → SecurityFilterChain (SecurityConfig) intercepts
  → BearerTokenAuthenticationFilter extracts token
  → NimbusJwtDecoder validates HS256 signature + expiry   ← shared secret
  → CampusOpsJwtAuthoritiesConverter maps "roles" claim → ROLE_STUDENT etc.
  → Authentication available to MVC
  → controller/CurrentUserResolver builds CurrentUser(subject, role)
```

Real config (`security/SecurityConfig.java`):

```java
.authorizeHttpRequests(a -> a
    .requestMatchers("/actuator/**", "/error").permitAll()
    .anyRequest().authenticated())
.oauth2ResourceServer(o -> o.jwt(jwt ->
    jwt.decoder(jwtDecoder()).jwtAuthenticationConverter(new CampusOpsJwtAuthoritiesConverter())))
```

Stateless everywhere (`SessionCreationPolicy.STATELESS`), CSRF off (no cookies/sessions). Custom entry point/accessDeniedHandler emit the same JSON `ErrorResponse` shape as MVC errors → uniform API failures: **401 = unauthenticated/invalid token; 403 = authenticated but not allowed**.

## 13.3 HS256 and the shared dev secret

HS256 = HMAC-SHA256 symmetric signing: same secret signs and verifies. Both resource servers hold the identical secret via `CAMPUSOPS_SECURITY_JWT_SECRET` (default documented dev value in properties).

Honest framing for interviews: symmetric means every verifier could also mint tokens — fine for two internal v1 services, replaced by asymmetric RS256 + real IdP in production.

## 13.4 DevTokenGenerator — and what does NOT exist

`security/dev/DevTokenGenerator.java` is a main-class utility minting signed demo tokens:

```powershell
java -cp <classes+deps> com.campusops.incident.security.dev.DevTokenGenerator MANAGER manager-1
# claims: sub=manager-1, roles=["MANAGER"], jti, iat, exp(+8h)
```

❌ **Not implemented in CampusOps v1 (intentional):** login endpoint, password database/user store, auth server, refresh tokens, key rotation, production IdP. Tokens exist so validation/authorization could be built realistically; the identity *provider* side was consciously deferred (ADR-004). Say this proactively — it's a scope decision, not an oversight.

**Common interview questions (Part 13):**
1. What's inside a JWT? Which parts does the signature cover? (Header.payload both; header declares alg.)
2. Why stateless auth suits microservices? (Any service with the secret validates locally; no shared sessions.)
3. Where do ROLE_* authorities come from here? (Converter over the `roles` claim.)
4. How would you migrate to RS256? (IdP signs; services keep only public key/JWKS — code change is just the decoder.)

---

# PART 14 — RBAC + ownership authorization

## 14.1 The actual implemented rules

| Action | STUDENT | TECHNICIAN | MANAGER |
|---|---|---|---|
| POST create | ✅ reporter forced = JWT sub | ❌ | ❌ |
| GET one/list | own incidents | assigned incidents | all |
| PUT details | own only | ❌ | any |
| PATCH status | own only (+ state machine) | assigned (+ state machine) | any (+ state machine) |
| PATCH assign | ❌ | ❌ | ✅ |

Create is **default-deny**: only STUDENT is approved, everyone else gets 403. The incident state machine (`canTransitionTo`) applies on top for every role — security never bypasses business rules.

## 14.2 Valid JWT ≠ permission

A perfectly valid TECHNICIAN token hitting `/assign` yields **403**, not success. Authentication answered "who"; authorization still asks "may you". Implemented in two layers:
- Coarse role gates: `@PreAuthorize("hasRole('MANAGER')")` on the assign endpoint.
- Everything ownership-shaped lives in `IncidentServiceImpl` (`authorizeReadAccess` / `authorizeUpdateAccess` / `authorizeProgressAccess`) because "own" requires reading the entity — impossible at URL level.

## 14.3 The reporterId vulnerability fix

Pre-Day 5, `reporterId` came from the request body — anyone could claim anyone's identity by typing a different ID. Now:

```text
request body reporterId ("hacker")  → IGNORED
JWT sub ("student-A")               → stored as reporterId
```

Covered by tests that literally send `"hacker-should-be-ignored"` and assert persistence shows the subject. Broader lesson: **never trust client-asserted identity fields**; identity comes from validated credentials only. Same reasoning makes `assigneeId` legitimate — there it is not the caller's identity but the *target technician chosen by the manager*.

**Common interview questions (Part 14):**
1. AuthN vs AuthZ with concrete CampusOps examples. (401 on missing JWT vs 403 on student calling assign.)
2. Why can't @PreAuthorize express all rules here? (Ownership needs entity data.)
3. How did you fix client-asserted identity? (Subject override story + test.)
4. What happens when priority changes on someone else's incident? (403 before any mutation — access check precedes mutation logic.)

---

# PART 15 — Feign JWT propagation

## 15.1 The problem

Asset Service is also a resource server. When Incident Service calls it during incident creation, Asset Service must authenticate *someone*. If the Feign call leaves without an `Authorization` header, Asset Service answers **401** — even though the original caller presented a perfectly valid token. Each hop must carry the credentials forward.

## 15.2 The mechanism

```java
// client/FeignAuthPropagationInterceptor.java
@Component
public class FeignAuthPropagationInterceptor implements RequestInterceptor {
    public void apply(RequestTemplate template) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            template.header("Authorization", "Bearer " + jwtAuth.getToken().getTokenValue());
        }
    }
}
```

Spring picks up any `RequestInterceptor` bean automatically and applies it to every Feign request. It reads the **current request's** validated JWT from the `SecurityContextHolder` and copies the exact header onto the outgoing call.

Full flow:

```text
Client ── Bearer JWT_A ──► Incident Service (validates JWT_A)
                              │ Feign copies Authorization: Bearer JWT_A
                              ▼
                        Eureka-resolved Asset Service (validates same JWT_A, same shared secret)
```

Same-token propagation works because both services trust the same secret and claims format — the caller's STUDENT role arrives intact at Asset Service.

## 15.3 Circuit breaker stays intact

The interceptor runs *before* the network attempt, inside the normal Feign pipeline — timeouts, breaker counting, and fallback behave exactly as before. Day 6 verification proved the whole chain live: asset created via MANAGER token, then a STUDENT incident referencing that asset returned 201 (resolution succeeded through discovery + propagated JWT); a broken chain would have surfaced as fallback placeholder or 500 instead.

**Common interview questions (Part 15):**
1. Why must tokens propagate between services? (Every hop re-validates; no implicit trust network.)
2. Alternative approaches? (Token exchange / service-to-service identities — heavier, ❌ not implemented; propagation of the user context keeps authorization end-to-end.)
3. How do you add headers to all Feign calls? (RequestInterceptor bean — this exact class.)

---

# PART 16 — Docker & Docker Compose

## 16.1 Concepts from zero

- **Image:** immutable template of an app + runtime. **Container:** a running instance of an image.
- **Dockerfile:** recipe to build an image, executed top-to-bottom; each instruction creates a **layer** (cached and reused until its inputs change).
- **Volume:** persistent storage managed by Docker that outlives containers.
- **Network:** Compose gives all services one network where the service name is a DNS name.
- **Port mapping:** `host:container` publishing, e.g. `5433:5432`.
- **Environment variable:** the way CampusOps reconfigures apps per environment without rebuilding.

## 16.2 The seven Compose services

```yaml
postgres(18)  mongo(7)  kafka(4.0 KRaft)  eureka-server
incident-service  asset-service  notification-service
```

Named volumes `campusops_postgres_data` / `campusops_mongo_data` keep database data across `docker compose down` (only `-v` destroys them). Postgres 18 detail worth knowing: its data mount must be at `/var/lib/postgresql`, not the old `/var/lib/postgresql/data`.

## 16.3 localhost ≠ localhost

Inside the incident container, `localhost` is **that container alone**. Hence in-container config uses service names (`postgres:5432`, `kafka:9092`, `eureka-server:8761`) while *host* tools keep familiar ports via mappings:

| Component | Host | Container-internal |
|---|---|---|
| Eureka | 8761 | eureka-server:8761 |
| Incident / Asset / Notification | 8082/8083/8084 | same |
| Kafka | **localhost:9092** → mapped to container 9094 (host listener advertising localhost:9092) | kafka:9092 |
| PostgreSQL | **5433** (avoids clashing native install) | postgres:5432 |
| MongoDB | **27018** | mongo:27017 |

Kafka's dual listeners exist precisely so both audiences hear correct advertised addresses (Part 10.4).

## 16.4 Multi-stage Dockerfiles

Every deployable has the same two-stage pattern:

```dockerfile
FROM eclipse-temurin:26-jdk AS build      # stage 1: build with bundled mvnw
COPY mvnw . ; COPY .mvn .mvn ; COPY pom.xml .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw   # CRLF fix for Windows checkout!
RUN ./mvnw -q -B dependency:go-offline || true    # dependency layer cache
COPY src src
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:26-jre               # stage 2: JRE-only runtime (+curl for healthchecks)
COPY --from=build /workspace/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

Why it matters: final image contains no JDK compiler/Maven/source; builds reproduce from clean checkout; the `sed` line exists because a Windows checkout carries CRLF line endings that break the Linux wrapper script — a real bug we hit. Final images carry curl solely so healthchecks can call Actuator.

## 16.5 Healthchecks & startup ordering

Each service defines a real readiness probe:

| Service | Healthcheck |
|---|---|
| postgres | `pg_isready -U postgres -d campusops_incident` |
| mongo | `mongosh --eval "db.adminCommand('ping')"` |
| kafka | `kafka-broker-api-versions.sh --bootstrap-server kafka:9092` |
| eureka | `curl -f http://localhost:8761/` |
| app services | `curl actuator/health \| grep '"status":"UP"'` |

App services then use:

```yaml
depends_on:
  postgres: { condition: service_healthy }
```

So "started" is never confused with "ready" — incident-service doesn't boot before its database accepts connections.

## 16.6 Configuration externalization

No Spring profiles or extra files: compose injects env vars mapped by relaxed binding (`SPRING_DATASOURCE_URL`, `SPRING_MONGODB_URI`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`, `CAMPUSOPS_SECURITY_JWT_SECRET`, SLA overrides). One jar artifact, any environment.

**Common interview questions (Part 16):**
1. Image vs container vs layer? (Template vs running instance vs cached build step.)
2. Why multi-stage? (Small runtime images, reproducible from clean checkout.)
3. How do containers find each other? (Compose network DNS by service name — never localhost.)
4. down vs down -v? (Keep volumes vs destroy data.)
5. What broke first time you ran the stack? (postgres:18 volume path + Eureka env naming — tell them as debugging stories.)

---

# PART 17 — Actuator + health

## 17.1 What it is

Spring Boot Actuator exposes production endpoints over HTTP. CampusOps exposes exactly two on each app service:

```properties
management.endpoints.web.exposure.include=health,info
```

- `/actuator/health` → aggregate status UP/DOWN (DB, disks, etc. contribute indicators automatically).
- `/actuator/info` → static info.
- ❌ Prometheus/Grafana/metrics endpoints/tracing: **not implemented** (explicitly rejected scope).

Security interplay: both resource servers permit `/actuator/**` unauthenticated so probes need no token; everything else stays protected. Only health+info are exposed — not the full management surface.

## 17.2 Why Docker cares

Compose healthchecks call these endpoints (via curl baked into the images) and gate startup with `depends_on.condition: service_healthy`. A container whose process started but whose DB connection fails reports DOWN and dependents never start against a half-ready service. The app healthchecks even grep for `"status":"UP"` rather than trusting HTTP 200 alone.

Liveness/readiness probe groups appear in the health payload (`{"groups":["liveness","readiness"]}`) — relevant when moving to Kubernetes, ❌ not exercised here beyond defaults.

**Common interview questions (Part 17):**
1. What is Actuator? Which endpoints did you expose and why only those?
2. How does your stack decide a service is ready? (Real health endpoint → Compose healthy → dependents start.)
3. Why should health endpoints be exempt from JWT auth here? (Probes run without credentials; exposure limited to health/info.)

---

# PART 18 — Testing

## 18.1 Vocabulary

- **Unit test:** one class in isolation; collaborators mocked. Fast, thousands-per-minute.
- **Integration test:** real Spring context + real database.
- **MockMvc:** drives HTTP requests through controllers (optionally with the real security filter chain) without a network port.
- **Mockito:** creates fake collaborators and verifies interactions.
- **@SpringBootTest:** boots the whole application context.

## 18.2 The actual suites (verified counts)

```text
incident-service     84 tests
asset-service        28 tests
notification-service 11 tests
               total 123
```

Highlights, mapped to what they protect:

| Test class (incident-service) | What it proves |
|---|---|
| `IncidentServiceTest` (19) | Business logic incl. reporter forcing, priority-change deadline recalculation |
| `SlaCalculatorTest` (5) | Every priority maps to configured duration |
| `IncidentAuthorizationTest` (15) | Full role×action ownership matrix incl. default-deny create |
| `api/IncidentApiSecurityTest` (8) | Real filter chain over MockMvc: no-token→401, garbage-token→401, cross-student→403, technician assign→403, manager assign→200 |
| `JwtInfrastructureTest` (6) | Mint↔decode round trip, wrong secret rejected, expired rejected, converter claim formats |
| `scheduler/SlaBreachScannerTest` (3) | Marks + publishes once; eligible-status query contract |
| `event/IncidentEventPublisherTest` (4) | Correct topic + key per event type |
| `repository/IncidentRepositoryTest` (4) | Real-Postgres persistence incl. breach-finder semantics (RESOLVED/breached/future excluded) |
| `service/SlaCalculatorTest`… plus controller (10), domain (5), client (4), context-load (1) | Layer contracts |

asset-service (28): controller/service/repository/domain + context load against real MongoDB. notification-service (11): consumer routing, mapping of all three events into notification rows, controller history API.

## 18.3 Patterns used

- Constructor-injection friendliness: mocks injected via `@InjectMocks`.
- `ArgumentCaptor` to assert published events' contents.
- Strict stubs (MockitoExtension) — unused stubbings fail tests (this actually caught a dropped asset-validation regression during Day 5).
- `KafkaTestConfig` provides a mocked `KafkaTemplate` and Kafka autoconfiguration is excluded in test properties — integration tests need Postgres, not a broker.
- Full-stack security tests mint real tokens via `DevTokenGenerator.mint(...)` — testing the true validation path.

## 18.4 Why not Testcontainers ❌

Plain suites already covered behavior; Testcontainers would add dependency weight + slower builds for v1 benefit we didn't need yet. It's the documented next step (spin real PG/Mongo/Kafka in CI). Deferred deliberately, like Flyway — say both in one sentence.

**Common interview questions (Part 18):**
1. Unit vs integration — examples from your project?
2. How do you test security rules end-to-end without a server? (MockMvc + real filter chain + minted tokens.)
3. Show me a bug your tests caught. (Day 5: UnnecessaryStubbing exposed removed asset-validation call; also the enum CHECK-constraint failure surfaced at runtime and became a schema lesson.)
4. Why are Kafka consumers tested with mocks instead of a broker?

---

# PART 19 — Complete request flows (learn these cold)

## FLOW 1 — Student creates an incident

1. **Request:** `POST /api/v1/incidents` + `Authorization: Bearer <STUDENT JWT>`, body `{title, description, priority, reporterId:"hacker"}`.
2. **Controller:** `IncidentController.createIncident` — `@Valid` DTO binding; `CurrentUserResolver` → `CurrentUser("student-A", STUDENT)`.
3. **Security:** filter already validated signature/expiry; roles converted.
4. **Service:** `IncidentServiceImpl.createIncident` — role≠STUDENT→403; body reporterId discarded; subject becomes reporter.
5. **Database/Feign:** assetId absent → skip Feign. `SlaCalculator.calculateDeadline(MEDIUM)` → deadline. `save()` inside `@Transactional`.
6. **Kafka:** commit → AFTER_COMMIT → `IncidentCreatedEvent` to `incident.created.v1` keyed by id.
7. **Transaction boundary:** commit precedes publish.
8. **Response:** `201 Created` JSON with id, status OPEN, slaDeadline, reporterId="student-A". Notification row appears asynchronously.

## FLOW 2 — Incident references an asset

Same as Flow 1 plus: service calls `AssetServiceClient.getAssetById(assetId)` → breaker CLOSED → Feign interceptor copies caller JWT → Eureka resolves `asset-service` → Asset Service validates same JWT → returns asset. Missing asset (404) → empty Optional → `IllegalArgumentException` → 400 to client. Breaker open → fallback placeholder → creation continues visibly degraded.

## FLOW 3 — Incident-created notification (async leg)

Notification Service's `IncidentEventConsumer.onIncidentCreated` receives the event from group `notification-service` → binds payload via default-type → `NotificationServiceImpl.recordIncidentCreated` in a new transaction → INSERT row (`INCIDENT_CREATED`, recipient=reporterId from event). Verify via `GET :8084/api/v1/notifications?incidentId=`.

## FLOW 4 — Manager assigns technician

`PATCH /{id}/assign`, MANAGER token. Security passes → `@PreAuthorize("hasRole('MANAGER')")` on controller passes → service: load incident, state machine OPEN→ASSIGNED valid, set assigneeId (= target technician from body — legitimately client-supplied since manager picks the assignee), save, publish `IncidentAssignedEvent`. TECHNICIAN/STUDENT tokens die at the annotation with 403.

## FLOW 5 — Technician progresses incident

`PATCH /{id}/status {IN_PROGRESS}`, TECHNICIAN token → ownership check: `assigneeId == subject` else 403 → state machine ASSIGNED→IN_PROGRESS valid else 400 → save. No event on plain status change (only create/assign/breach produce events).

## FLOW 6 — SLA breach

Every 30s: `SlaBreachScanner.scanForBreachedIncidents` (@Transactional) → breach finder (active, eligible status, past deadline, unmarked) → per hit: mark `slaBreachedAt`, publish event → commit → Kafka `incident.sla-breached.v1` → consumer → SLA_BREACHED notification for reporter. Exactly-once per incident via marker. RESOLVED/CLOSED never match.

## FLOW 7 — JWT-protected request (security-first view)

Missing/garbage token → filter chain rejects before MVC → 401 JSON. Valid token but wrong role for endpoint → `@PreAuthorize` or service check → 403 JSON. Valid + allowed → business proceeds. Uniform ErrorResponse bodies from either layer.

## FLOW 8 — JWT propagation Incident → Asset

Covered in Flows 2 and Part 15: `FeignAuthPropagationInterceptor` copies `SecurityContextHolder`'s validated JWT onto the outgoing request; Asset Service re-validates with the shared secret; breaker/fallback unaffected.

---

# PART 20 — Failure scenarios (expected behavior → why → component)

| Scenario | Expected behavior | Why | Component |
|---|---|---|---|
| JWT missing | 401 JSON ErrorResponse | No credentials = no authentication | SecurityConfig entry point |
| JWT expired | 401 (invalid_token) | Nimbus validates exp | JwtDecoder |
| JWT signed with wrong secret | 401 | Signature mismatch | JwtDecoder |
| Wrong role for action | 403, resource untouched | @PreAuthorize / service checks run before mutation | Controller annotation + service guards |
| Student reads another student's incident | 403 | Ownership predicate fails | authorizeReadAccess |
| Technician touches unassigned incident | 403 | assigneeId ≠ subject | authorizeProgressAccess |
| Asset Service down during create (with assetId) | Creation still succeeds; fallback placeholder name stored | Breaker opens after threshold; fallback supplies degraded data | AssetServiceClient + Resilience4j |
| Asset Service returns 404 | 400 "Asset not found…" immediately | 404 mapped as answer, not failure (no breaker noise) | AssetServiceClient catch |
| Eureka down | Existing flows keep working via cached registry; fresh lookups degrade; registration retries | Client-side cache + heartbeat model | Eureka clients |
| Kafka down at incident creation | Incident persists; publish fails/logged; that event is lost (❌ outbox not implemented) | AFTER_COMMIT send is best-effort after durable commit | IncidentEventPublisher |
| Kafka down at breach scan | Breach marked & committed; events lost this cycle | Same AFTER_COMMIT cost; dedup prevents refire later | SlaBreachScanner |
| PostgreSQL down (incident) | Health DOWN; Compose gates dependents; requests fail fast at pool | Real datasource health indicator + healthcheck gating | Actuator + compose |
| MongoDB down (asset) | Same pattern for asset-service only | Isolated blast radius | Actuator + compose |
| Scanner runs twice / concurrently | Second scan finds nothing (marker filtered) | Idempotency by DB state | slaBreachedAt finder |
| Duplicate event delivered | At-least-once duplicate possible; v1 tolerates simple inserts | Consumer offsets + no dedup key (documented limitation) | notification consumer |
| Rollback after publish attempt | No event ever sent | AFTER_COMMIT listener never invoked | TransactionPhase.AFTER_COMMIT |
| Docker container restarts | Data survives (volumes); identity re-registers; consumers resume from committed offsets | Volumes + registry heartbeats + offset bookmarks | Compose/Eureka/Kafka |

---

# PART 21 — Architecture tradeoffs ("why not X?")

Answer pattern to memorize: **constraint → choice → accepted cost**.

- **Why microservices instead of monolith?** Learning goal + genuinely different data shapes and failure profiles; accepted network/debugging cost. At real campus scale, a well-modularized monolith would also be defensible — say so.
- **Why PostgreSQL AND MongoDB?** Transactional lifecycle vs flexible equipment attributes. *(ADR-001)*
- **Why Eureka?** Name-based discovery without config edits; client-side caching keeps working through registry hiccups. *(ADR-002)*
- **Why Feign?** Declarative clients, seamless Eureka LB + interceptor hooks for JWT. *(ADR-002)*
- **Why Resilience4j?** Contain the one sync dependency's failure; fallback preserves availability. *(ADR-002)*
- **Why Kafka?** Decoupled, replayable facts; KRaft drops ZooKeeper ops burden. *(ADR-003)*
- **Why AFTER_COMMIT?** Never announce rolled-back facts. Accepted residual risk: post-commit send loss (outbox ❌). *(ADR-003)*
- **Why JWT without auth server?** v1 scope forbade new services/user stores; dev minting let us build *real* validation+authorization. Replacement path documented. *(ADR-004)*
- **Why HS256 not RS256?** Symmetric simplicity for two internal verifiers; production → RS256 + JWKS. *(ADR-004)*
- **Why service-layer authorization?** Ownership predicates need entities; annotations can't read rows. *(ADR-005)*
- **Why scheduled scanner?** State-driven, restart-safe, zero infra; latency ≤ interval is acceptable for SLAs. *(ADR-006)*
- **Why Docker Compose, not Kubernetes?** One-command reproducible demo stack; nothing to autoscale yet. *(ADR-007)*
- **Why no Testcontainers/Flyway/Redis/Gateway/tracing?** Each deferred consciously with a trigger condition written down (CI portability / enum-pain already felt / no cache use-case / single entry-point need / debugging beyond logs).

---

# PART 22 — Interview question bank

Concise answers included where the question is common; project-specific ones reference real components.

## Java / Spring Boot
- **B:** What is DI? What is a bean? Which stereotypes did you use?
- **B:** What does `@SpringBootApplication` combine?
- **I:** Constructor vs field injection — why constructor everywhere in CampusOps? (Explicit, immutable, test-friendly.)
- **I:** How do you externalize config per environment? (Relaxed-binding env vars over properties.)
- **PS:** Walk me through `IncidentServiceImpl`'s dependencies and why each exists.
- **Trap:** "Isn't Lombok hiding your constructors?" (No — it *generates* the same constructor injection; tests inject mocks through it.)

## REST
- **B:** GET vs POST vs PUT vs PATCH — where does CampusOps use each?
- **B:** Meaning of the status codes your API returns?
- **I:** Path variable vs query parameter — examples from `/incidents`.
- **I:** How is validation wired? (`@Valid` + constraints + advice → 400 with field map.)
- **PS:** Why does PATCH exist for status/assign but PUT for details?
- **Trap:** "Why return the entity after create?" (Client needs id + computed slaDeadline.)

## JPA / Hibernate
- **B:** Entity vs DTO vs repository?
- **I:** `@Enumerated(EnumType.STRING)` — why mandatory here?
- **I:** Derived query methods — read your breach finder aloud and translate it to SQL semantics.
- **PS:** What did ddl-auto=update NOT do for you? (Constraint alteration → 23514 story.)
- **Trap:** "Is `save()` always an INSERT?" (Merge/insert by id state — v1 uses IDENTITY so new entities insert.)

## PostgreSQL
- **B:** Primary key, constraint, transaction, ACID.
- **I:** Where are transactions bounded in Incident Service? (Class readOnly + method-level write.)
- **PS:** Which columns did Day 4 add and how did schema change? (sla_deadline/sla_breached_at via update.)
- **Trap:** "How do you keep notification.incidentId consistent with incidents.id?" (Eventual consistency by design — copied id from event, no cross-service FK.)

## MongoDB
- **B:** Document/collection/flexible schema.
- **I:** What goes into asset `attributes` and why not columns?
- **PS:** Which property name binds Mongo in Boot 4.1 and what bit you? (`spring.mongodb.uri` / SPRING_MONGODB_URI.)

## Microservices
- **B:** Monolith vs microservices; database-per-service.
- **I:** How do services stay consistent without distributed transactions? (Events + eventual consistency; no sagas ❌ — name it as future work for multi-service writes.)
- **PS:** Why doesn't Notification register with Eureka?

## Eureka
- **B:** Registration/heartbeat/discovery cycle.
- **I:** Client-side vs server-side discovery; where's the cache?
- **PS:** Recount the env-var naming bug and its fix.

## Feign
- **B:** What problem does a declarative client solve?
- **I:** Logical service name resolution path.
- **PS:** Why wrap Feign in AssetServiceClient instead of using it raw? (Breaker + 404-as-answer mapping live there.)

## Resilience4j
- **B:** Three states; what trips OPEN here (numbers!).
- **I:** Breaker vs retry; fallback design choice (visible placeholder).
- **PS:** Why is 404 excluded from breaker counting?

## Kafka
- **B:** Topic/partition/offset/group.
- **I:** How is ordering achieved per incident? (Key = incidentId.)
- **I:** Delivery semantics of this system? (At-least-once; duplicates tolerated; exactly-once ❌.)
- **PS:** Name the three topics and who consumes them.
- **Trap:** "Kafka is down — does incident creation fail?" (No: commit wins, event lost — explain AFTER_COMMIT cost honestly.)

## Transactions
- **I:** Default rollback rules of @Transactional? (Unchecked exceptions.)
- **PS:** Explain AFTER_COMMIT with the scanner example.
- **Trap:** "AFTER_COMMIT listener throws — does the DB roll back?" (No — commit already happened; send failures are logged/lost.)

## JWT / Spring Security
- **B:** Signature covers what? Where do claims come from?
- **I:** Stateless meaning; why no sessions/CSRF here.
- **PS:** How are ROLE_* authorities produced? (Converter over roles claim.)
- **Trap:** "Base64 means encrypted, right?" (No — encoding; signature gives integrity, not confidentiality; HTTPS provides secrecy.)

## RBAC
- **PS:** State the full matrix from memory (Part 14 table).
- **PS:** Why default-deny create for MANAGER? (Only explicitly approved rules shipped; loosening is a one-line change.)
- **Trap:** "Valid token, wrong role — what status?" (403; walk the layers.)

## Docker / Compose
- **B:** Image/container/layer/volume/port-map.
- **I:** Why healthcheck-gated depends_on beats plain depends_on.
- **PS:** Dual Kafka listeners — draw them.
- **Trap:** "Container can reach your native Postgres on localhost:5432?" (No — namespace lesson; that's why host maps are 5433/27018.)

## Testing
- **I:** Unit vs integration split in numbers (84/28/11).
- **PS:** How do you test the authorization matrix end-to-end? (MockMvc + real chain + minted tokens — IncidentApiSecurityTest.)
- **Trap:** "Tests need Kafka running?" (No — mocked template + autoconfig excluded; Postgres/Mongo are real though.)

## System design (generic)
- Design a food-delivery order tracker / hotel booking SLA monitor — reuse: state machine, deadline marker dedup, events-after-commit, ownership-scoped reads.

---

# PART 23 — Rapid revision

## CampusOps in 5 minutes

Three business services + registry. Incident (:8082, Postgres) owns lifecycle/state machine/priority-SLA/breach scanning/authz; Asset (:8083, Mongo) owns equipment docs; Notification (:8084, Postgres) consumes three Kafka topics into rows and serves history (unauthenticated internal). Sync edge (incident→asset) = Feign+Eureka+breaker+JWT propagation. Async edges = AFTER_COMMIT-published events keyed by incident id. Security = HS256 resource servers, subject=identity, roles STUDENT/TECHNICIAN/MANAGER, coarse annotations + service-layer ownership, manager-only assignment, default-deny create. SLA = priority→duration at create, recalc on priority change, 30s scanner marks+publishes once via slaBreachedAt dedup. Deployment = 7-service Compose, multi-stage Temurin 26 builds, named volumes, healthcheck-gated starts, env-var configuration (SPRING_MONGODB_URI / EUREKA_CLIENT_SERVICEURL_DEFAULTZONE lessons). Tests 84/28/11=123 incl full-stack security matrix. Deliberately absent: IdP/login, RS256, gateway, Redis, K8s, tracing, Flyway, Testcontainers.

## 50 things to explain without looking

1–5 Problem statement; service list+ports; which DB per service; database-per-service rule; notification not in Eureka.
6–10 Layer responsibilities; DTO vs entity; GlobalExceptionHandler mappings; ErrorResponse shape; validation wiring.
11–15 AuthN vs AuthZ; 401 vs 403 paths; roles claim→authorities; CurrentUser record purpose; subject-overrides-body rule.
16–20 Full auth matrix; default-deny create; assigneeId legitimacy; state machine order; transition failure code (400).
21–25 SlaCalculator mapping; deadline recalc-on-change; scanner cadence+query; slaBreachedAt dedup; RESOLVED/CLOSED exclusion.
26–30 Three topics+consumers; key=incidentId rationale; acks=all; type-header-free contract binding; group-id/offset reset.
31–35 AFTER_COMMIT mechanics; phantom-event danger story; scanner transaction; post-commit loss honesty; fallbackExecution subtlety.
36–40 Feign logical-name resolution; interceptor propagation; breaker states+numbers; 404-vs-failure mapping; fallback placeholder visibility.
41–45 Compose seven services; dual Kafka listeners; 5433/27018 host maps; healthcheck gating; multi-stage+CRLF fix.
46–50 Test totals+key suites; MockMvc-with-chain technique; mocked-Kafka test approach; two property-naming war stories; deferred-items list (IdP/RS256/Flyway/Testcontainers/gateway/tracing).

## Top 30 interview questions

1. Walk me through your architecture. (Part 1.8 + Part 23 summary)
2. Why microservices here? 3. Why these DB technologies? 4. Sync vs async decision rule + examples.
5. What happens on POST /incidents? (Flow 1 verbatim.) 6. How is reporter identity established? 7. Authorization matrix?
8. Valid JWT but unauthorized — trace it. 9. How do services discover each other? 10. What if Asset Service is down?
11. Circuit breaker states with your thresholds. 12. Why Feign interceptors? 13. Kafka topics and consumers?
14. Ordering guarantees in Kafka? 15. Delivery semantics + duplicates? 16. Why AFTER_COMMIT? 17. Rollback after publish attempt?
18. Scanner double-run safety? 19. Priority change effects? 20. RESOLVED exclusion reason?
21. JWT structure + validation path? 22. HS256 trade-off + production migration? 23. Why no login server in v1?
24. Ownership checks placement rationale? 25. reporterId vulnerability story? 26. JWT propagation mechanism?
27. Health gating in Compose? 28. Multi-stage build benefits + CRLF gotcha? 29. down vs down -v?
30. What would you build next and why? (IdP→RS256, Flyway after enum pain, Testcontainers for CI.)

## 10 whiteboard flows/diagrams to draw

1. Big-box architecture (services, DBs, Eureka, Kafka, client).
2. POST /incidents end-to-end sequence incl. AFTER_COMMIT fork.
3. Breach lifecycle timeline (deadline → scan → mark → event → row).
4. JWT validation filter path with role conversion.
5. Ownership check decision tree per role×action.
6. Feign call with breaker around it (CLOSED/OPEN/HALF_OPEN transitions annotated).
7. Kafka dual-listener topology (container vs host audiences).
8. Compose startup DAG (healthcheck edges).
9. Multi-stage Dockerfile layer flow.
10. Failure map: what breaks when each infra piece dies.

---

# PART 24 — Scenario-based system design questions (frequently asked on this project)

These are the "what would you do if…" prompts interviewers attach to projects like this. Each gives a strong answer built from CampusOps' actual parts.

**S1. "Your SLA scanner must notify within seconds of breach, not up to 30s later. Redesign."**
Keep persistence identical; replace polling trigger with per-incident scheduling: on creation/priority-change, enqueue a delayed message (Kafka doesn't do delays natively → use a delayed-queue store or scheduled DB job per incident) or compute on write. Trade-offs to voice: timer-per-incident memory, crash recovery (scanner stays as reconciliation backstop — hybrid model), exactly-once still guaranteed by `slaBreachedAt`.

**S2. "Incident creation currently writes Postgres then publishes Kafka. A crash between commit and publish loses the event. Fix it."**
Transactional outbox: write `outbox` row in the SAME transaction (event payload + status), separate relay polls outbox → publishes → marks sent. Guarantees commit⇔event atomicity. Mention CDC/Debezium as the production-grade variant, and that v1 accepted this gap knowingly (documented in study guide Part 11.4).

**S3. "Two technicians get assigned the same incident concurrently. Prevent it."**
Assignment already flows through one row update guarded by the state machine — make the transition conditional: `UPDATE incidents SET assignee_id=?, status='ASSIGNED' WHERE id=? AND status='OPEN'`; zero affected rows → 409 Conflict. Discuss optimistic locking (`@Version`) as the general tool. v1 didn't need it because single-manager demos never raced — say that honestly.

**S4. "Scale to 50k incidents/day. What breaks first?"**
Single Kafka broker/partition (ordering per incident survives via keys, throughput ceiling first), single Postgres instance (connection pool + index on sla_deadline/status), scanner query growing unindexed. Order of fixes: partitioned topics + consumer scaling (group already supports it), DB indexes/migrations, multiple app instances behind Eureka (stateless already), then K8s. Note Compose→K8s is config work since apps are already stateless+externalized-config.

**S5. "Add a 'comment' feature where students and technicians discuss an incident. Which service owns it?"**
Incident Service owns comments (they're lifecycle-adjacent data of the incident aggregate) OR a fourth discussion service consuming incident events — defend either, but justify with ownership + access rules reusing the existing role matrix (student-own / technician-assigned / manager-any). Notifications for new comments ride the existing topic pattern (`incident.commented.v1`).

**S6. "Requirement: managers must receive an email on CRITICAL breach. Email provider has 99% uptime and 2s latency."**
Never inline into the scanner/request path. Extend Notification Service: on SLA_BREACHED consume, persist row (already done) + hand off to an outbound email worker with retry queue; delivery is best-effort async. This is exactly why notifications were made event-driven on Day 3.

**S7. "Security review: tokens are dev-minted HS256 with a shared secret. Give a 90-day hardening plan."**
Phase 1: real IdP (OIDC) issuing tokens; services switch decoder to JWKS (code touch ≈ decoder bean only — resource-server pattern pays off). Phase 2: RS256 + rotation windows. Phase 3: secrets to vault; compose stops carrying any secret. Phase 4: gateway enforces auth at edge; services keep defense-in-depth (current matrix unchanged). Map each phase to documented limitation.

**S8. "Notification Service is down for an hour. Impact?"**
Zero user-facing impact on reporting/assignment; events accumulate in topics (retention); on restart the consumer resumes from committed offsets and drains the backlog — at-least-once may duplicate → note v1 tolerance and the idempotency-key upgrade. This answer showcases why the async split exists.

**S9. "Prove no student can read another student's incidents — mechanically."**
Three layers: (1) list queries scoped by `findByReporterIdAndActiveTrue(subject)`; (2) single-read predicate `authorizeReadAccess`; (3) `IncidentApiSecurityTest` cross-student case expecting 403 with real filter chain. Plus negative-create test proving body reporterId ignored. Offer the matrix test class as executable evidence.

**S10. "Interviewer: 'This should have been a monolith.' Respond."**
Agree partially: at current scale a modular monolith would ship faster. Defend the choice by learning objectives + genuinely divergent data models + independent failure domains, then show maturity by listing what the split cost (duplicated event DTOs, distributed debugging, two DB engines) and what would trigger consolidation (small team, single deploy target). Never be defensive — tradeoff fluency is the point.

---

*End of study guide. Sources: repository state verified Day 7 · README.md · docs/adr/001–007 · PROJECT_CONTEXT.md §20.*








