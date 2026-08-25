# ADR-005: RBAC plus service-layer ownership authorization

## Status
Accepted (Day 5)

## Context
Before Day 5, identity fields like `reporterId` were client-asserted request-body values — any caller could claim to be anyone. Possessing a valid JWT must not automatically grant access, and ownership rules ("students see only their own incidents") cannot be expressed by URL patterns alone because they depend on entity state.

## Decision
- The JWT `sub` is the **only** trusted caller identity. On creation, `reporterId` is forced from the token subject; body-supplied values are ignored.
- **Coarse gating** uses `@PreAuthorize` where a rule is purely role-shaped (assignment is `hasRole('MANAGER')`).
- **Ownership and business checks live in the service layer** (`IncidentServiceImpl`) via a small `CurrentUser` abstraction (subject + role) resolved once in the controller:
  - STUDENT: create; view/update/progress own incidents.
  - TECHNICIAN: view/progress incidents assigned to them; may not update details; may never assign.
  - MANAGER: full visibility, update/progress, assignment rights.
  - Create is default-deny for non-STUDENT roles — anything not explicitly approved is forbidden.
- Authorization failures raise `ForbiddenException` / Spring's `AccessDeniedException`, both handled centrally in the existing `GlobalExceptionHandler`, producing consistent 403 `ErrorResponse` payloads.

## Consequences
- Ownership logic can inspect real entity state (reporterId, assigneeId) — impossible with annotations alone.
- Rules are unit-testable as a plain matrix (15 service-level cases) plus full-stack MockMvc security tests.
- List endpoints scope themselves through existing repository finders — no new queries required.
- Assignment remains MANAGER-only; technicians cannot self-assign or reassign.

## Alternatives considered / rejected
- **Annotation-only security** — cannot express "own incident" predicates.
- **Security checks in controllers** — scatters business rules outside the service layer.
- **Separate authorization service** — overkill for v1; rules are tightly coupled to incident entities anyway.
