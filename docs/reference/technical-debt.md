# Technical Debt and Known Trade-offs

Current milestone: `v0.14.0 — Minimal observability and operational readiness`.

This document lists known limitations and future improvements. The goal is to make trade-offs explicit rather than hide them.

## Summary

The project is an educational portfolio project, not a commercial booking platform.

Current priorities:

- keep architecture understandable
- demonstrate clean boundaries
- show reliable integration patterns
- cover important invariants with tests
- make distributed flows observable locally

Some production concerns are intentionally deferred.

## Observability

### Implemented

- Actuator health/readiness/metrics endpoints
- MDC logging context
- HTTP correlation id support
- gRPC context propagation
- Kafka consumer context restoration
- notification delivery context restoration
- custom Micrometer business metrics
- gRPC client deadlines

### Not implemented yet

- Prometheus deployment and scrape config
- Grafana dashboards
- centralized log aggregation
- JSON log profile for production ingestion
- OpenTelemetry tracing
- alerting rules

### Future improvement

- add `compose.observability.yaml` with Prometheus and Grafana
- add dashboard for saga outcomes, outbox failures and notification delivery
- add JSON logging profile for production-like environments
- add OpenTelemetry instrumentation for HTTP, gRPC and Kafka boundaries
- add alerts for terminal outbox failures and repeated saga failures

## Saga and payment process

### Customer cancellation after approved payment

Current customer cancellation cancels booking and inventory reservation, but a full refund/reversal process for already approved payments is not implemented yet.

Future process:

```text
customer cancellation
  -> cancel confirmed inventory reservation
  -> refund or reverse payment
  -> mark booking cancelled
  -> publish BookingCancelled / PaymentRefunded events
  -> notify user
```

Future improvement:

- add payment refund operation
- add payment states such as `REFUNDED` or `REFUND_FAILED`
- model cancellation/refund as a separate process manager or saga
- define retry and compensation behavior for refund failures

### Spring Statemachine is a comparison prototype

The state machine implementation is intentionally kept as a comparison endpoint.

It is not the default production-like implementation.

Reason:

- the handmade process manager is simpler for the current workflow size
- the state machine adds useful comparison value
- both implementations reuse the same saga actions
- replacing the main implementation without a stronger need would add complexity without clear benefit

## Inventory and concurrency

### Inventory hold expiration

Inventory holds are currently released explicitly by saga compensation or confirmed by the happy path.

Not implemented yet:

- automatic hold expiration
- `expiresAt` on hold records
- scheduled cleanup of abandoned holds
- optional `InventoryHoldExpired` event

Future improvement:

- add expiration timestamp
- add scheduled expiration job
- make expiration idempotent
- add metrics for expired/released holds

### Multi-day reservation rollback

Inventory hold placement uses atomic per-date updates.

For a multi-day stay:

```text
reserve day 1
reserve day 2
reserve day 3
...
```

If a later date cannot be reserved, previously reserved dates are released.

This is pragmatic for the current milestone, but it is not a multi-document transaction.

Future improvement:

- consider MongoDB transactions if required
- add reconciliation job for inconsistent availability state
- add retryable rollback handling
- add metrics for rollback failures

## Testing

### Full multi-service end-to-end test

Current testing focuses on important slices:

- inventory-level contention with real MongoDB Testcontainers
- booking-level saga contention with real PostgreSQL Testcontainers and controlled inventory/payment test doubles
- module-level unit tests for domain/application behavior

Not implemented yet:

```text
real booking-service
real inventory-service
real payment-service
real notification-service
real audit-service
real Kafka
real PostgreSQL
real MongoDB
all running together in one automated full-stack test
```

Reason:

The current tests protect critical invariants without making the build heavy and brittle.

Future improvement:

- add a separate system-test module
- start all services with Testcontainers or Docker Compose
- verify HTTP/gRPC/Kafka boundaries together
- add contract tests for booking -> inventory gRPC and booking -> payment HTTP

## Notification

### Logging sender only

The current notification sender writes messages to logs.

This is intentional for local/demo usage:

- no external credentials
- deterministic behavior
- easy to verify in tests and logs

Future improvement:

- add real email sender
- add Telegram sender
- add Max sender
- keep logging sender as local adapter
- add sender-specific failure/retry scenarios

### Notification events are not published yet

Notification-service does not currently publish `notification.events`.

Future improvement:

- publish `NotificationCreated`, `NotificationSent`, `NotificationFailed`, `NotificationSkipped`
- optionally project notification events into audit timeline
- add metrics for skipped and failed notifications

## Security

### Dev security remains intentionally simple

Local `dev` profile uses development-friendly security where appropriate. Booking-service uses a fixed demo user (`dev@example.com`) and inventory admin endpoints use a local demo administrator.

Production-like Google JWT support exists for booking-service, but the local demo focuses on architecture and integration flow.

Future improvement:

- document real frontend login flow when a frontend/BFF appears
- add more integration tests for authorization rules
- add a clearer admin role management story

### Internal service security

Inventory gRPC supports TLS/mTLS with local development certificates. The certificate generation path is documented, but production-grade certificate lifecycle is intentionally out of scope for this milestone.

Future improvement:

- certificate rotation strategy
- deployment-oriented mTLS notes
- optional SPIFFE/SPIRE evaluation
- security audit events for sensitive admin operations

## CI and repository setup

### Branch protection

GitHub Actions CI exists, but branch protection depends on repository settings.

Future improvement:

- protect `master`
- require CI to pass before merge
- optionally require pull request review

### CI job split

Current CI can remain simple:

```bash
./gradlew check --no-daemon --stacktrace
```

Future improvement if build time grows:

- split unit tests and integration tests
- publish test reports more explicitly
- add dependency vulnerability scanning
- tune Gradle build cache

## API and product scope

The project focuses on backend architecture, not a full product.

Not implemented yet:

- frontend UI
- real user registration flow
- real payment provider
- refund process
- hotel search ranking
- pricing engine
- promotion/discount logic
- admin UI

Future milestone:

```text
v0.15.0 — Minimal Demo UI
```

The UI should stay small and demonstrate the existing backend flow rather than expand product scope too much.

