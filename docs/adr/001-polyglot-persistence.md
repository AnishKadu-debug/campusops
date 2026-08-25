# ADR-001: Polyglot persistence — PostgreSQL and MongoDB

## Status
Accepted (Day 1)

## Context
CampusOps has two clearly different data domains:

- **Incidents** (and later notifications) are relational and transactional: a strict lifecycle state machine, priority-derived SLA deadlines, breach markers, and audit timestamps. Integrity constraints and ACID transactions matter here.
- **Assets** are heterogeneous physical equipment. A projector has `resolution` / `lampHours`; a router has `ip` / `firmware` / `ports`. Forcing these shapes into one relational schema means sparse nullable columns or constant schema churn.

Each microservice owns its database; crossing service database boundaries is forbidden.

## Decision
- Incident Service → **PostgreSQL** (`campusops_incident`)
- Notification Service → **PostgreSQL** (`campusops_notification`)
- Asset Service → **MongoDB** (`campusops_asset`) with a flexible `attributes` map for type-specific fields.

## Consequences
- Each domain uses the storage model that fits its access patterns.
- Two operational database technologies to run, back up, and understand.
- Schema evolution strategies differ per service (relational DDL vs document flexibility).

## Alternatives considered / rejected
- **PostgreSQL for everything**, storing asset attributes as JSONB — workable in production, rejected here because the asset domain genuinely fits documents and the project explicitly wanted a justified polyglot decision rather than uniformity.
- **MongoDB for everything** — rejected: incident lifecycle integrity (state transitions, deadlines, audit) is exactly where ACID transactions and constraints pay off.
