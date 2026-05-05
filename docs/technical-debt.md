# Technical Debt

## Current version

`v0.5.2`

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
  -> preparation for transactional outbox, Kafka, mTLS and saga
```

---

## Current implemented booking flow

```text
create booking
  -> place inventory hold
  -> booking becomes ON_HOLD

confirm booking
  -> confirm inventory hold
  -> held rooms become booked rooms
  -> booking becomes CONFIRMED

cancel ON_HOLD booking
  -> release inventory hold
  -> booking becomes CANCELLED

cancel CONFIRMED booking
  -> release booked inventory rooms
  -> booking becomes CANCELLED
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

### Separate service applications

The project is now a Gradle multi-project build with two separately runnable Spring Boot applications:

```text
apps/booking-service-app
apps/inventory-service-app
```

Booking and inventory are still kept in the same repository, but they run as separate applications.

---

### gRPC inventory boundary

Booking-to-inventory communication goes through gRPC.

Allowed dependency:

```text
booking -> inventory-grpc-api
```

Forbidden dependency:

```text
booking -> inventory domain/application
```

The gRPC contract is owned by:

```text
modules/inventory-grpc-api
```

---

### PostgreSQL persistence for booking

Booking state is persisted in PostgreSQL.

The booking persistence adapter is owned by the booking module.

---

### MongoDB persistence for inventory

Inventory data is persisted in MongoDB.

MongoDB currently stores hotels, room availability and room holds.

---

### Booking security and ownership

The booking service supports Google JWT authentication in the `security-jwt` profile.

An authenticated Google user is mapped to an internal application user.

Bookings are owned by internal `UserId` values, not directly by Google accounts.

The intended mapping is:

```text
Google JWT subject/email
  -> app_users
  -> internal UserId
  -> Booking.userId
```

Booking ownership checks are enforced by the booking application layer.

---

### Explicit local profiles

Starting from `v0.5.2`, local development profiles are activated explicitly.

The default application configuration should not silently start in development security mode.

Local development is started with:

```text
--spring.profiles.active=local
```

---

## v0.5.2 technical debt snapshot

### Cross-service consistency

Booking and inventory are persisted in different databases.

Current booking flow still performs synchronous inventory operations and booking persistence in separate resources.

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

Planned improvements:

```text
v0.6.0 transactional outbox
v0.7.0 Kafka event publication
v0.10.0 booking process manager / saga
```

The transactional outbox will not solve cross-service atomicity by itself.
It will guarantee that booking state changes and booking events are persisted atomically in the booking database.

The saga/process manager will address the larger distributed workflow.

---

### Transaction boundaries

Booking use cases need explicit transaction boundaries.

This becomes critical when the transactional outbox is introduced.

Target rule for `v0.6.0`:

```text
booking state change + outbox insert
must be committed in the same PostgreSQL transaction
```

Possible implementation options:

```text
- use Spring @Transactional on application services
- introduce an application-level TransactionRunner port
```

For the current learning stage, Spring `@Transactional` on booking application services is acceptable.

---

### Service-to-service security

Booking-to-inventory gRPC communication is not yet protected by mTLS.

Target model:

```text
external users -> Google JWT
internal services -> mTLS
```

Inventory JWT is not the target model for service-to-service communication.

Planned for `v0.6.2`:

```text
- inventory gRPC server TLS
- client certificate requirement
- booking gRPC client certificate
- internal CA for local development
- validation of booking-service identity from the client certificate
```

---

### Inventory HTTP admin authentication

Inventory administrative HTTP endpoints are protected by `ROLE_ADMIN`.

In local development this is provided by the `security-dev` profile and a mock admin user.

This is intentionally not a production-grade admin authentication model.

Future options:

```text
- Google JWT based admin access
- separate admin frontend
- BFF-level authorization
- internal-only admin API
```

This is not urgent before outbox, Kafka, mTLS and saga.

---

### Public catalog vs admin API

Users should be able to browse hotels, room types and availability before login.

Target public endpoints:

```text
GET /api/v1/hotels
GET /api/v1/hotels/{hotelId}
GET /api/v1/hotels/{hotelId}/room-types/{roomTypeId}/availability
```

Administrative write endpoints should remain under:

```text
/api/v1/admin/**
```

This separation should be preserved when the frontend is introduced.

---

### Inventory reservation identity

Booking currently stores `holdId` only while the booking is in `ON_HOLD`.

After confirmation, the hold id is cleared.

Confirmed booking cancellation is currently performed by:

```text
hotelId + roomTypeId + stayPeriod + rooms
```

This is simple and works for the current learning stage, but it is not ideal for saga, audit and compensation.

Future improvement:

```text
introduce inventoryReservationId
keep stable reservation identity after confirmation
use it for cancellation, audit and saga compensation
```

Possible future model:

```text
InventoryReservation
  id
  hotelId
  roomTypeId
  stayPeriod
  rooms
  status: HELD / CONFIRMED / CANCELLED / EXPIRED
```

Booking would store:

```text
inventoryReservationId
```

---

### Inventory concurrency

Inventory availability updates are not yet protected from concurrent lost updates.

Risk example:

```text
availableRooms = 1

Request A reads heldRooms = 0
Request B reads heldRooms = 0

A saves heldRooms = 1
B saves heldRooms = 1

Two holds exist, but availability shows only one held room
```

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

Examples:

```text
- repeated create booking requests can create multiple bookings
- repeated confirm requests may produce domain errors
- repeated cancel requests may produce domain errors
- repeated Kafka events are not yet handled
```

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

### Outbox and event publication

The project does not yet have transactional outbox support.

Planned for `v0.6.0`:

```text
- booking_outbox table
- booking lifecycle events
- event envelope
- event versioning
- booking transaction boundary
```

Planned for `v0.6.1`:

```text
- outbox polling publisher
- retries
- status transitions
- failure handling
- locking strategy
```

Planned for `v0.7.0`:

```text
- Kafka infrastructure
- publish booking events from outbox
- topic naming convention
- dead-letter topic strategy
```

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

Possible statuses:

```text
CREATED
APPROVED
DECLINED
CANCELLED
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

If audit is down, producers and business services should continue working.

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
v0.6.0
  transactional outbox and booking lifecycle events

v0.6.1
  outbox polling publisher and retries

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
