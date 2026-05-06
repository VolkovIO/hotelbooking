# Technical Debt

## Current version

`v0.6.2`

The project is a learning-oriented backend for hotel booking.

The main goal is to practice Clean Architecture, DDD tactical patterns, explicit module boundaries and the gradual transition from a modular monolith toward distributed services.

The project is intentionally not production-ready yet.

Current architectural focus:

```text
separate booking and inventory service applications
  -> explicit business module boundaries
  -> PostgreSQL persistence for booking
  -> MongoDB persistence for inventory
  -> gRPC boundary between booking and inventory
  -> Google JWT for external booking API access
  -> booking ownership checks
  -> transactional outbox foundation
  -> outbox polling publisher with logging adapter
  -> preparation for Kafka, mTLS and saga
```

---

## Current implemented booking flow

```text
create booking
  -> place inventory hold
  -> booking becomes ON_HOLD
  -> booking outbox event is stored
  -> polling publisher logs the event
  -> outbox row becomes PUBLISHED

confirm booking
  -> confirm inventory hold
  -> held rooms become booked rooms
  -> booking becomes CONFIRMED
  -> booking outbox event is stored
  -> polling publisher logs the event
  -> outbox row becomes PUBLISHED

cancel ON_HOLD booking
  -> release inventory hold
  -> booking becomes CANCELLED
  -> booking outbox event is stored
  -> polling publisher logs the event
  -> outbox row becomes PUBLISHED

cancel CONFIRMED booking
  -> release booked inventory rooms
  -> booking becomes CANCELLED
  -> booking outbox event is stored
  -> polling publisher logs the event
  -> outbox row becomes PUBLISHED
```

A cancelled booking is not physically deleted. Cancellation is represented by the `CANCELLED` status.

---

## Completed improvements

### Module boundaries

Booking and inventory are separated into explicit modules.

Booking no longer depends directly on inventory domain objects. Integration goes through booking outbound ports and inventory published contracts.

Current booking outbound ports:

```text
InventoryLookupPort
InventoryReservationPort
```

The adapter between booking and inventory acts as an anti-corruption layer.

It maps inventory-side application results and exceptions to booking-side contracts.

---

### Transactional outbox foundation

Starting from `v0.6.0`, the booking service stores booking lifecycle events in a transactional outbox.

Implemented:

```text
booking_outbox table
booking lifecycle event model
booking outbox repository
booking state + outbox event persistence in one PostgreSQL transaction
```

Current event types:

```text
BookingPlacedOnHold
BookingConfirmed
BookingCancelled
```

The transaction boundary is intentionally local to PostgreSQL:

```text
Booking aggregate save
  + booking_outbox insert
```

This is now handled atomically.

---

### Outbox polling publisher

Starting from `v0.6.1`, the project has a polling outbox publisher with a logging adapter.

Implemented:

```text
- claiming NEW events as PROCESSING
- logging adapter publication
- PUBLISHED status transition
- retryable failures
- terminal FAILED status
- SKIP LOCKED based batch claiming
```

The current publisher does not send messages to Kafka yet.

The logging adapter exists to validate outbox mechanics before Kafka is introduced.

---

## v0.6.1 technical debt snapshot

### Cross-service consistency

Booking and inventory are persisted in different databases.

Current booking flow still performs inventory operations before booking state persistence.

Known risk:

```text
inventory operation succeeds
booking persistence fails
=> booking and inventory may become inconsistent
```

Examples:

```text
place inventory hold succeeds
booking save fails
=> orphan inventory hold

confirm inventory hold succeeds
booking save fails
=> inventory and booking statuses diverge

release inventory succeeds
booking cancellation save fails
=> inventory and booking statuses diverge
```

The transactional outbox does not solve cross-service atomicity by itself.

It guarantees only this local atomic operation:

```text
booking state change
  + booking outbox event insert
```

The larger distributed consistency problem is intentionally left for the saga/process manager work.

Planned improvements:

```text
v0.7.0 Kafka event publication
v0.10.0 booking process manager / saga
```

---

### Outbox publication

The project now has the first outbox polling publisher.

Implemented:

```text
- outbox table
- lifecycle event model
- atomic booking state + outbox persistence
- polling publisher
- status transitions
- retryable failure handling
- terminal failure handling
- logging adapter
```

Not implemented yet:

```text
- Kafka publication
- dead-letter topic
- consumer inbox/idempotency
```

Planned for `v0.7.0`:

```text
- Kafka producer
- booking.events topic
- event envelope publication
- dead-letter strategy
```

---

### Transaction boundaries

Booking state and outbox event persistence now have a dedicated transactional boundary.

The important rule is:

```text
bookingRepository.save(...)
bookingOutboxRepository.save(...)
```

must either both commit or both roll back.

Current implementation keeps the PostgreSQL transaction away from remote gRPC calls.

Current order:

```text
call inventory
  -> change booking state
  -> persist booking and outbox in one local transaction
```

This avoids long database transactions around network operations.

Remaining issue:

```text
the inventory operation is still outside the booking database transaction
```

This is expected until saga/process manager work.

---

### Event contract maturity

Current booking outbox events are application-level lifecycle events.

They are not yet part of a full event contract strategy.

Future improvements:

```text
- event envelope standardization
- correlationId
- causationId
- event schema documentation
- event compatibility rules
- Kafka topic naming convention
```

Current event version:

```text
1
```

Event versioning should be preserved as consumers are introduced.

---

### Service-to-service security

Booking-to-inventory gRPC communication is protected by local development mTLS.

Implemented:
```text
- inventory gRPC server TLS
- client certificate requirement
- booking gRPC client certificate
- local development CA
- booking-service client identity check
```

Remaining future work:
```text
- certificate rotation strategy
- production certificate management
- possible SPIFFE/SPIRE evaluation
- service-level tests for rejected unauthenticated clients
```

---

### Inventory reservation identity

Booking currently stores `holdId` only while the booking is in `ON_HOLD`.

After confirmation, the hold id is cleared.

Future improvement:

```text
introduce inventoryReservationId
keep stable reservation identity after confirmation
use it for cancellation, audit and saga compensation
```

---

### Inventory concurrency

Inventory availability updates are not yet protected from concurrent lost updates.

Future improvements:

```text
- optimistic locking for Mongo documents
- conditional updates for availability reservation
- retry policy for version conflicts
- repository contract tests for concurrent reservation scenarios
```

This should be addressed before the project is considered portfolio-ready.

---

### Idempotency

Current command operations are not idempotent.

Future improvements:

```text
- HTTP Idempotency-Key for booking creation
- eventId-based consumer deduplication
- inbox table for Kafka consumers
- idempotent payment command handling
- idempotent notification delivery handling
```

This becomes especially important after Kafka and payment are introduced.

---

### Inbox pattern

After Kafka consumers are introduced, consumers should be idempotent.

Future consumer services:

```text
notification-service
payment-service
audit-service
booking-process-manager
```

Possible table:

```text
processed_events
  event_id
  consumer_name
  processed_at
```

This prevents duplicate processing when Kafka redelivers an event.

---

### Notification delivery

Notification service is planned as an extensible service.

Target channels:

```text
EMAIL
TELEGRAM
MAX
```

Notification failures should not cancel a successfully confirmed booking.

Target principle:

```text
booking success does not depend on notification delivery success
```

Notification delivery should have its own status and retry model.

---

### Payment service

Payment service is planned as a symbolic educational service.

It does not need real bank integration.

It should demonstrate:

```text
- payment aggregate
- approve / decline / cancel operations
- payment status transitions
- payment events
- failure path that triggers saga compensation
```

---

### Saga / process manager

Saga is planned after payment service foundation.

Target approach:

```text
orchestrated saga / booking process manager
```

Reason:

```text
- explicit process state
- explicit compensation logic
- easier to explain in interviews
- better for learning than pure choreography
```

The first implementation should be a lightweight custom process manager, not Temporal or Camunda.

Temporal/Camunda may be evaluated later.

---

### Audit

Audit is planned as a minimal symbolic service.

It should consume cross-cutting events and store audit records.

Audit should not block the main booking/payment process.

---

### Observability and resilience

The project does not yet have centralized observability.

Planned future work:

```text
- structured logs
- correlationId and causationId in events
- OpenTelemetry tracing
- Prometheus metrics
- Grafana dashboards
- ELK/OpenSearch logs
- gRPC deadlines
- gRPC retries
- Kafka consumer metrics
```

---

### Contract and integration testing

Current tests cover many domain and application scenarios, but service-level testing should be extended.

Future improvements:

```text
- Testcontainers for PostgreSQL
- Testcontainers for MongoDB
- Testcontainers for Kafka
- gRPC boundary tests
- service-level integration tests
- event contract tests
- repository contract tests
```

---

### Documentation and ADRs

Architecture decisions should be documented as ADRs.

Suggested ADRs:

```text
ADR-001: Modular architecture and bounded contexts
ADR-002: PostgreSQL for booking and MongoDB for inventory
ADR-003: Transactional outbox
ADR-004: mTLS for service-to-service communication
ADR-005: Saga orchestration over choreography
ADR-006: Notification failures do not cancel bookings
ADR-007: Symbolic payment provider for educational MVP
```

---

## Roadmap

```text
v0.6.2
  inventory gRPC mTLS

v0.7.0
  Kafka infrastructure and event publication

v0.8.0
  notification service foundation

v0.9.0
  symbolic payment service

v0.10.0
  booking process manager / saga and compensation

v0.11.0
  frontend or BFF with Google login

v0.12.0
  audit service

v0.13.0
  observability and resilience

v0.14.0
  service-level integration tests, CI, diagrams, ADRs

v1.0.0
  portfolio-ready release
```
