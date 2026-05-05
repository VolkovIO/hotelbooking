# Technical Debt

## Current version

`v0.6.0`

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
  -> preparation for outbox publisher, Kafka, mTLS and saga
```

---

## Current implemented booking flow

```text
create booking
  -> place inventory hold
  -> booking becomes ON_HOLD
  -> booking outbox event is stored

confirm booking
  -> confirm inventory hold
  -> held rooms become booked rooms
  -> booking becomes CONFIRMED
  -> booking outbox event is stored

cancel ON_HOLD booking
  -> release inventory hold
  -> booking becomes CANCELLED
  -> booking outbox event is stored

cancel CONFIRMED booking
  -> release booked inventory rooms
  -> booking becomes CANCELLED
  -> booking outbox event is stored
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

## v0.6.0 technical debt snapshot

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
v0.6.1 outbox polling publisher
v0.7.0 Kafka event publication
v0.10.0 booking process manager / saga
```

---

### Outbox publication

The project has the first transactional outbox foundation, but it does not publish events yet.

Implemented:

```text
- booking_outbox table
- booking lifecycle event model
- booking outbox repository
- atomic booking state + outbox persistence
```

Not implemented yet:

```text
- outbox polling publisher
- event status transitions after publication
- retries
- failure handling
- Kafka publication
- dead-letter topic
```

Planned for `v0.6.1`:

```text
- polling publisher
- batch selection
- retry attempts
- status transitions
- failure recording
- SKIP LOCKED based locking
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

This is not urgent before outbox publisher, Kafka, mTLS and saga.

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
- repeated future Kafka events will need deduplication
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
