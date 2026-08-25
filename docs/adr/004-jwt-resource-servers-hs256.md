# ADR-004: JWT resource servers with a shared HS256 development secret

## Status
Accepted (Day 5)

## Context
Day 5 required real authentication and authorization, but the v1 scope explicitly excludes an auth server, Keycloak, OAuth login flows, refresh tokens, password storage, and additional microservices. There is no user store anywhere in the system.

## Decision
- Incident Service and Asset Service are **OAuth2 resource servers** (`spring-boot-starter-oauth2-resource-server`), stateless, validating **HS256** JWTs via `NimbusJwtDecoder` with a **shared secret** read from configuration (`campusops.security.jwt.secret`, overridable through `CAMPUSOPS_SECURITY_JWT_SECRET`). The value is a documented development secret, never presented as production material.
- Tokens carry `sub` (caller identity) and a simple `roles` claim (`STUDENT`, `TECHNICIAN`, `MANAGER`), converted to Spring `ROLE_*` authorities.
- Tokens are produced by a clearly separated **development utility** (`DevTokenGenerator`, main-class) used only for local development, demos, and tests. It is not reachable over HTTP and is not a login system.
- `/actuator/**` and `/error` are the only permitted paths; everything else requires a valid JWT.

## Consequences
- Real signature/expiry validation with zero identity infrastructure.
- Anyone with the dev secret can mint any token — acceptable locally, unacceptable in production.
- Secret distribution across services is manual (compose env) — fine at two services.

## Alternatives considered / rejected
- **Dedicated auth microservice** — cleanest long-term, but violates the approved "no additional microservices" v1 scope.
- **Keycloak / external IdP** — heavyweight for the learning timeline; noted as roadmap.
- **A login endpoint with configured users/passwords inside Incident Service** — invents a fake user store inside a bounded context that does not own identity.
- **RS256** — better production posture but requires key generation/distribution machinery v1 intentionally skips.
