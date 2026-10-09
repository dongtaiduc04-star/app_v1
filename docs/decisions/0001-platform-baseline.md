# ADR-0001: Platform baseline

- **Status:** Proposed
- **Date:** 2026-08-25

## Decision

Use a Java 17 Maven monorepo containing three independently deployable Spring
services and one Next.js frontend. Use one MySQL server with isolated service
databases, Redis for sessions/rate limits, RabbitMQ for asynchronous events, and
Flyway for schema ownership. Expose backend traffic only through the gateway.

Use transactional outboxes and idempotent consumers for lifecycle and analytics
events. Keep deployment URLs and secrets outside source control. Produce four
independent container images tagged with the Git commit SHA.

## Consequences

The design preserves service boundaries and independent images but increases
local resource use and test complexity. Cross-service consistency is eventual.
The existing single-container Helm chart cannot represent this architecture and
will require a separately approved, minimal migration in phase 11.

## Open decisions

- Confirm whether only Super Admin can restore a soft-deleted account during the
  30-day retention period.
- Recover the previous CI/CD workflow or explicitly approve rebuilding it.
