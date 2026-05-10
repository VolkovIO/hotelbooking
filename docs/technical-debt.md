# Technical Debt

This document tracks known limitations and future improvements for the hotel booking project.

Current milestone: `v0.11.0`.

## Booking saga

### 1. Cancellation after approved payment is not implemented

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

This is intentionally postponed to avoid making the project too broad before the saga comparison milestone is complete.

---

### 2. Inventory hold expiration is not implemented

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

### 3. Payment approval unknown outcome is not reconciled

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
- add stronger correlation/causation ids

---

### 4. Retry handling is basic

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
- no dead-letter table for permanently failed sagas
- no operator UI
- no manual resume endpoint
- no distributed lock for multi-instance retry scheduler beyond current DB safeguards

Future improvement:

- exponential backoff
- jitter
- retry reason classification
- operator/admin endpoints
- failed saga dashboard
- alerting on `FAILED` sagas

---

### 5. Spring Statemachine prototype is not the default production-like flow

In `v0.11.0`, Spring Statemachine is added as a runnable comparison prototype.

Endpoint:

```http
POST /api/v1/bookings/saga-statemachine
```

Profile:

```text
booking-saga-springstatemachine-prototype
```

Limitations:

- not the default booking saga flow
- retry behavior is not as complete as the handmade process manager
- durability still depends on existing `booking_sagas` persistence
- state machine itself is not persisted as an independent workflow history
- no production decision has been made to replace handmade saga

Purpose:

The prototype exists to compare orchestration styles, not to replace the current implementation.

---

### 6. Temporal is documented but not implemented

Temporal is a production-grade durable workflow engine, but it is not implemented in `v0.11.0`.

Reason:

- requires Temporal server/dev server
- requires workers
- requires workflow/activity programming model
- introduces separate infrastructure
- would significantly increase project scope

Future improvement:

Possible future milestone:

```text
v0.12.x or later:
  prototype Temporal booking workflow in a separate module/branch
```

The current project documents Temporal as an architectural alternative only.

---

## Outbox and Kafka

### 7. Booking outbox polling is simple

Current booking outbox publisher works and publishes booking events to Kafka.

Limitations:

- no advanced metrics
- no outbox lag dashboard
- no poison-message quarantine beyond retry/error fields
- no admin reprocess endpoint

Future improvement:

- Micrometer metrics
- outbox lag gauge
- published/failed counters
- reprocess endpoint for failed messages
- alerting on stuck outbox records

---

### 8. Kafka producer logs can be noisy on first initialization

Kafka client logs print large producer configuration blocks when producer is first created.

This is not a functional issue.

Future improvement:

- tune logging levels for Kafka internals in local profile
- keep business logs visible while reducing infrastructure noise

---

## Notification service

### 9. Notification senders are logging adapters only

Current notification-service supports notification flow end-to-end, but real external delivery is not implemented.

Implemented:

- preference API
- notification persistence
- delivery status
- scheduler
- logging sender
- Kafka booking event consumer

Not implemented:

- real Telegram sender
- real Max sender
- real email sender

Decision:

This is intentional for the educational project. The goal was to complete the full internal cycle without spending time on external provider integrations.

Future improvement:

- Telegram adapter
- Max adapter
- SMTP/email adapter
- provider-level retry/backoff
- delivery receipts if supported

---

## Observability

### 10. Distributed tracing is not implemented

The system uses logs and correlation ids in events, but there is no full tracing setup yet.

Missing:

- OpenTelemetry tracing
- trace propagation over HTTP/gRPC/Kafka
- spans for saga steps
- dashboard for saga execution path

Future improvement:

- add OpenTelemetry
- propagate trace id/correlation id through booking, inventory, payment, notification
- add saga step span attributes
- visualize end-to-end flow

---

### 11. Metrics are limited

Future useful metrics:

- saga started/completed/compensated/failed counters
- retry count distribution
- outbox publication lag
- notification delivery lag
- payment declined rate
- inventory hold failure rate

---

## Security

### 12. Internal service authentication is incomplete

Some service-to-service integrations are intentionally simple in local development.

Future improvement:

- mTLS or signed service tokens for internal HTTP/gRPC calls
- stronger identity propagation
- endpoint-level authorization for admin operations

---

## Documentation debt

### 13. Diagrams must be kept in sync with implementation

The project now has Mermaid diagrams for booking saga and workflow comparison.

Risk:

Architecture diagrams can become stale when code changes.

Future improvement:

- update diagrams whenever saga steps change
- keep manual verification examples current
- document differences between handmade saga and Spring Statemachine prototype
