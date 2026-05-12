# Technical Debt

This document tracks known limitations and future improvements for the hotel booking project.

Current milestone: `v0.12.0 — Integration tests + concurrency safety + CI`.

## Testing and CI

### 1. Full multi-service end-to-end contention test is not implemented

Current coverage:

- inventory-level contention test with real MongoDB Testcontainers
- booking-level saga contention test with real PostgreSQL Testcontainers
- controlled inventory and payment test doubles in the booking-level test

Not implemented yet:

```text
real booking-service
real inventory-service
real payment-service
real PostgreSQL
real MongoDB
real Kafka
real gRPC calls
all running together in one full system test
```

Decision for now:

The current split is intentional.

```text
Inventory test proves the real inventory atomic invariant.
Booking test proves saga behavior under controlled inventory rejection.
```

Future improvement:

- add full-stack e2e tests using Docker Compose or dedicated system-test module
- verify HTTP/gRPC boundaries with real running service applications
- add Kafka assertions for booking events
- add notification assertions for downstream side effects

---

### 2. Branch protection is not configured yet

GitHub Actions CI exists and runs Gradle checks.

However, repository branch protection may not yet require CI to pass before merge.

Future improvement:

- protect `master`
- require `CI / Build and test` before merge
- optionally require pull request reviews
- optionally require branches to be up to date before merge

Decision for now:

Keep branch protection as a repository setup step after CI is stable.

---

### 3. CI runs one broad Gradle check job

Current CI command:

```bash
./gradlew check --no-daemon --stacktrace
```

This is simple and understandable.

Possible future improvements:

- split CI into separate jobs for unit tests, integration tests, and static analysis
- publish test reports more explicitly
- enable Gradle build cache optimizations
- consider `--parallel` if stable
- add dependency vulnerability scanning later

Decision for now:

Keep CI simple until the project has more tests and the feedback time becomes a real bottleneck.

---

## Inventory and concurrency

### 4. Inventory hold reservation uses application-level rollback for multi-day stays

Inventory hold placement now uses atomic per-date updates.

For a multi-day stay:

```text
reserve day 1 atomically
reserve day 2 atomically
reserve day 3 atomically
...
```

If one date cannot be reserved, previously reserved dates are released.

This is a pragmatic approach for the current project.

Limitation:

```text
It is not a multi-document database transaction.
```

Risk:

If rollback fails unexpectedly, a partial hold reservation could remain.

Current mitigation:

- rollback failures are logged
- `RoomHold` is created only after all per-date reservations succeed

Future improvement:

- consider MongoDB transactions if replica-set/transaction semantics are required
- add reconciliation job for inconsistent availability state
- make rollback attempts retryable
- add metrics/alerts for rollback failures

---

### 5. Inventory hold expiration is not implemented

Inventory holds are currently released explicitly by saga compensation.

Implemented:

- `placeHold`
- `confirmHold`
- `releaseHold`
- `cancelConfirmedReservation`

Not implemented yet:

- automatic hold expiration
- `expiresAt` on hold
- scheduled cleanup of abandoned holds
- `InventoryHoldExpired` event

Future improvement:

- add expiration timestamp to inventory hold
- add scheduled expired hold release
- make release idempotent
- publish optional inventory expiration event

---

### 6. Confirm/release/cancel inventory operations can be hardened further

The current milestone focuses on atomic hold placement because that is the critical last-room contention invariant.

Future improvements:

- review confirm hold behavior under concurrency
- review release hold idempotency
- review confirmed reservation cancellation under concurrency
- add integration tests for confirm/release/cancel race scenarios
- add optimistic or atomic state transitions for hold records if needed

---

## Booking saga

### 7. Booking-level contention test uses controlled test doubles

`BookingSagaContentionIntegrationTest` does not start the real inventory-service or payment-service.

It uses:

- real booking-service Spring context
- real PostgreSQL
- fake inventory reservation port
- fake inventory lookup port
- fake payment client

Reason:

```text
The test focuses on booking saga behavior after inventory contention result is known.
```

Future improvement:

- add full e2e saga contention test with real services
- add contract tests between booking-service and inventory-service
- verify gRPC error mapping in integration tests

---

### 8. Cancellation after approved payment is not implemented

Current booking saga handles rollback during booking creation.

It supports:

- payment declined before booking confirmation
- inventory hold release
- booking cancellation during compensation

It does not yet implement a separate business process for customer cancellation after successful payment approval.

Current successful final state:

| Component | State |
|---|---|
| Booking | `CONFIRMED` |
| Payment | `APPROVED` |
| BookingSaga | `COMPLETED` |

If a customer later cancels an already confirmed and paid booking, a new process is needed:

```text
Customer cancellation
  -> cancel confirmed inventory reservation
  -> refund/reverse payment
  -> mark booking cancelled
  -> publish BookingCancelled / PaymentRefunded events
  -> notify user
```

Future improvement:

- add payment refund or reversal operation
- add payment state such as `REFUNDED` or `REFUND_FAILED`
- create a separate cancellation/refund process manager or saga
- define compensation/retry behavior for refund failure

Decision for now:

This is intentionally postponed to avoid making the project too broad before core testing, observability, and portfolio documentation are stronger.

---

### 9. Payment approval unknown outcome is not reconciled

If booking-service calls payment-service `approve(paymentId)` and loses the HTTP response, the real payment state may be unknown.

Current behavior:

- retry is basic
- payment-service is expected to be idempotent
- no reconciliation job exists yet

Risk:

```text
payment approval may have succeeded remotely
booking-service may think the step failed
```

Future improvement:

- add payment status query endpoint
- add reconciliation job for unknown payment outcomes
- make approve/cancel idempotent by command id or operation id
- add stronger correlation/causation IDs

---

### 10. Retry handling is basic

The handmade saga supports retry metadata:

- `WAITING_RETRY`
- `retryCount`
- `nextAttemptAt`
- retry scheduler

Current retry scope:

- retryable payment technical failures
- retryable inventory technical failures

Limitations:

- no exponential backoff
- no jitter
- no dead-letter process for permanently failed sagas
- no operational dashboard

Future improvement:

- add retry policy abstraction
- add backoff and jitter
- add saga failure metrics
- add admin/retry endpoint or operational repair command

---

## Observability

### 11. Correlation and causation IDs are not fully implemented

Current events and logs do not yet provide a complete traceable chain across all services.

Future improvement:

- add `correlationId`
- add `causationId`
- propagate IDs across HTTP, gRPC, Kafka, and logs
- include IDs in error responses
- use IDs in audit/event timeline service

This is planned for a future audit/timeline or observability milestone.

---

### 12. Metrics and tracing are not implemented yet

Not implemented yet:

- Prometheus/Grafana dashboards
- OpenTelemetry distributed tracing
- Kafka consumer metrics
- saga outcome metrics
- payment outcome metrics
- notification metrics
- centralized log search through ELK/Loki

Future improvement:

- add Micrometer metrics for saga results
- add structured JSON logs
- add OpenTelemetry traces across booking -> inventory -> payment -> Kafka -> notification
- optionally add ELK or Loki profile for centralized log search

---

## API and product scope

### 13. API error model can be improved

Current APIs use basic error responses.

Future improvement:

- introduce consistent error response format
- add stable business error codes
- include `correlationId` in error responses
- improve OpenAPI examples
- document expected business failures

Decision for now:

Postponed because testing, concurrency safety, and observability provide more interview value at this stage.

---

### 14. Frontend/BFF is postponed

A lightweight frontend or BFF can be useful for demos.

Possible future scope:

- Google login
- booking demo flow
- booking status screen
- booking event timeline
- Swagger remains available

Decision for now:

Postpone until backend testing, audit/timeline, and observability are stronger.

---

### 15. Temporal is documentation-only for now

Temporal is documented as a production-grade workflow alternative, but is not implemented in code.

Reason:

- requires Temporal server
- requires worker runtime
- requires workflow/activity programming model
- changes the architecture significantly

Decision for now:

Keep Temporal as a future experiment, not part of the main project path.
