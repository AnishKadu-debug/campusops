# ADR-002: Eureka + OpenFeign + Resilience4j for synchronous communication

## Status
Accepted (Day 2)

## Context
Creating an incident that references an asset requires an **immediate** answer: does this asset exist? This is a request/response dependency, not an event. Hardcoding `asset-service` URLs is brittle, and a slow or down Asset Service must not cascade into Incident Service failures.

## Decision
- Both services register with **Eureka** (`eureka-server:8761`) and discover each other by logical name.
- Incident Service calls Asset Service through **OpenFeign** (`@FeignClient(name = "asset-service")`).
- The Feign call site is wrapped in a **Resilience4j circuit breaker** (`assetService` instance, count-based sliding window, failure-rate threshold) with a fallback returning a placeholder asset (`"UNAVAILABLE (Circuit Breaker Fallback)"`), so incident creation degrades gracefully instead of failing outright.

## Consequences
- No hardcoded URLs; instances scale/relocate without configuration changes.
- Asset outages trip the breaker and hit the fallback; incident creation keeps working with degraded asset information.
- Adds registry + client-side load balancing infrastructure to operate.
- Feign calls carry the caller's JWT (see ADR-004/005).

## Alternatives considered / rejected
- **Hardcoded URLs / static config** — brittle, no load balancing.
- **Kafka-based asset lookup** — wrong tool: the caller needs a synchronous answer during validation.
- **Plain RestTemplate without breaker** — loses timeout isolation and cascading-failure protection.
