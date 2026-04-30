# Technical Debt

## Current version

`v0.3.0`

The project is currently a learning modular monolith focused on Clean Architecture, DDD tactical patterns, explicit module boundaries and the gradual transition toward separately runnable services.

The main implemented booking flow is:

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

The project now has:

- PostgreSQL persistence for booking
- MongoDB persistence for inventory
- gRPC boundary between booking and inventory in the main PostgreSQL + MongoDB profile
- direct Java in-process inventory client kept only as a temporary compatibility profile

The project is still intentionally not production-ready.

Transaction boundaries, cross-module consistency, optimistic locking, idempotency, retries, outbox/events and observability are not fully addressed yet.

---

## Completed improvements

### Module boundaries

Booking and inventory are separated into explicit modules.

Booking no longer depends directly on inventory domain objects. Integration goes through booking outbound ports.

Current booking outbound ports:

- `InventoryLookupPort`
- `InventoryReservationPort`

Current inventory published use cases:

- `InventoryQueryUseCase`
- `InventoryReservationUseCase`

The adapter between booking and inventory acts as an anti-corruption layer.

---

### Module-specific HTTP exception handling

HTTP exception handling is now split by module.

Booking has booking-specific API error handling.

Inventory has inventory-specific API error handling.

This reduces coupling between HTTP adapters and prepares the project for future service separation.

Inventory-specific exceptions should not leak through booking HTTP flows.

---

### Booking-inventory lookup contract

Booking no longer asks inventory several low-level questions such as:

- whether a hotel exists
- whether a room type exists
- what guest capacity the room type has

Instead, booking requests a single room type reference required for its use case.

This reduces coupling between modules and prepares the boundary for transport-based integration.

---

### Inventory gRPC boundary

The main PostgreSQL + MongoDB profile now routes booking-to-inventory communication through gRPC.

Runtime flow:

```text
REST / Swagger
  -> BookingController
  -> Booking application use case
  -> InventoryLookupPort / InventoryReservationPort
  -> booking gRPC client adapter
  -> inventory gRPC server adapter
  -> Inventory application use case
```

The application is still a single Spring Boot runtime.

The gRPC boundary was introduced before splitting the runtime into separate applications. This allows the contract to be validated while the system remains easy to run and debug.

Current gRPC-related profiles:

- `inventory-grpc-server`
- `inventory-grpc-client`

Temporary direct integration profile:

- `inventory-direct-client`

---

### Cancellation flow

The cancellation flow now supports both:

- cancelling an `ON_HOLD` booking
- cancelling a `CONFIRMED` booking

Cancelling an `ON_HOLD` booking releases the inventory hold.

Cancelling a `CONFIRMED` booking releases booked inventory rooms.

A cancelled booking is not physically deleted. Cancellation is represented by the `CANCELLED` status.

---

## Known technical debt

### Temporary direct inventory client

The old direct Java in-process inventory client is still present behind the `inventory-direct-client` profile.

It is kept temporarily for:

- legacy in-memory integration tests
- comparison with the gRPC integration style
- easier transition during the current architecture phase

This direct client should be removed in the next architecture step.

Planned removal:

```text
v0.4.0
  remove in-memory runtime profile
  remove direct booking-to-inventory Java adapters
  keep booking-to-inventory communication through gRPC only
```

---

### Transaction boundaries

Booking creation currently performs an inventory hold before saving the booking.

If inventory hold creation succeeds but booking persistence fails, an orphan inventory hold may remain.

Confirmed booking cancellation updates inventory first and then saves the booking.

If booking persistence fails after inventory is updated, booking and inventory may become inconsistent.

This is acceptable for the current learning stage, but must be addressed before treating the system as production-like.

Possible future options:

- single database transaction while modules remain in one runtime
- optimistic locking
- transactional outbox
- saga or process manager if modules become distributed services
- idempotency keys for externally retried commands

---

### Cross-module consistency after gRPC

The gRPC boundary makes the booking-inventory integration more explicit, but it does not solve consistency by itself.

The current flow is still synchronous:

```text
booking
  -> inventory gRPC call
  -> booking persistence update
```

Potential failure scenarios remain:

- inventory hold is created but booking persistence fails
- inventory hold is confirmed but booking status update fails
- inventory booked rooms are released but booking cancellation persistence fails
- gRPC call succeeds but client receives a timeout
- client retries an operation that was already applied by inventory

Future improvements should address:

- idempotent inventory commands
- retry-safe booking commands
- outbox events
- saga/process manager
- consistent failure recovery strategy

---

### Inventory reservation identity

The current model clears `holdId` after booking confirmation.

Confirmed booking cancellation is currently performed by:

```text
hotelId + roomTypeId + stayPeriod + rooms
```

This is simple and works for the current learning stage.

A future improvement may introduce a more explicit inventory reservation identity, for example:

```text
inventoryReservationId
```

This would allow booking to keep a stable reference to the inventory reservation after confirmation.

Possible future model:

- place inventory reservation
- keep `inventoryReservationId` in booking
- confirm inventory reservation
- cancel inventory reservation
- keep reservation history for audit

---

### RoomAvailability mutability style

`RoomAvailability` currently mostly uses immutable-style behavior where operations return a new instance.

Examples:

- `placeHold(...)`
- `releaseHold(...)`
- `confirmHold(...)`
- `releaseBookedRooms(...)`

The project should decide whether inventory entities should consistently use:

- mutable entity-style methods
- immutable replacement-style methods

This should be clarified before adding more complex persistence and concurrency behavior.

---

### Repository contract tests

Current tests mostly verify domain behavior and application service behavior.

Now that real persistence adapters exist, repository contract tests should be introduced and aligned across implementations.

The same contract should be reusable for different implementations.

Example:

- `InMemoryBookingRepositoryTest`
- `JdbcBookingRepositoryTest`
- `InMemoryHotelRepositoryTest`
- `MongoHotelRepositoryAdapterTest`
- `InMemoryRoomAvailabilityRepositoryTest`
- `MongoRoomAvailabilityRepositoryAdapterTest`
- `InMemoryRoomHoldRepositoryTest`
- `MongoRoomHoldRepositoryAdapterTest`

The goal is to make sure all repository implementations preserve the same observable behavior.

---

### In-memory runtime profile

The in-memory runtime profile was useful during early development.

It now creates extra maintenance cost because the main runtime path has moved toward:

```text
PostgreSQL + MongoDB + gRPC
```

The in-memory runtime profile should be removed in `v0.4.0`.

After removal, tests should either:

- use focused unit tests with explicit fakes
- use persistence-backed integration tests
- use gRPC-backed application/service-level integration tests

---

### Idempotency

Current command operations are not idempotent.

Examples:

- confirming an already confirmed booking is rejected
- cancelling an already cancelled booking is rejected
- repeated HTTP or gRPC calls may produce domain errors

This is acceptable for now.

Real distributed systems usually need safer retry behavior, for example:

- idempotency keys
- command identifiers
- request deduplication
- process state tracking
- retry-aware gRPC clients
- idempotent inventory reservation operations

---

### gRPC error model

The current gRPC error model is intentionally simple.

Inventory server-side exceptions are mapped to gRPC statuses.

Booking gRPC client-side failures are mapped back to booking application exceptions.

This keeps Java exception types from leaking across module boundaries.

Future improvements may introduce:

- stable application error codes
- explicit error details
- richer mapping between gRPC status and HTTP responses
- separate handling for validation, business conflicts and service unavailability
- `google.rpc.ErrorInfo` or similar structured error details

---

### Error model

Inventory-specific exceptions should not leak through booking flows.

Booking should expose booking-level errors to API clients.

Current behavior:

```text
inventory exception
  -> inventory gRPC status
  -> booking gRPC client adapter
  -> booking application exception
  -> booking HTTP error response
```

Possible future improvements:

- introduce stable application error codes
- separate domain errors from integration errors
- improve error response consistency across modules
- map inventory unavailability to `503 Service Unavailable`

---

### Observability

The project currently has minimal application-level logging.

Infrastructure logs from Spring Boot, PostgreSQL, MongoDB and Liquibase are visible, but business and integration flows are not yet consistently observable.

Future improvements should include:

- structured logs
- correlation IDs
- request IDs
- gRPC metadata propagation
- Spring Boot Actuator health checks
- Micrometer metrics
- OpenTelemetry tracing
- gRPC deadline and timeout logging
- clear logs for booking lifecycle transitions

This is planned for a later observability/resilience milestone.

---

### Java version and gRPC/Netty warnings

Java 21 LTS is recommended for local development and future CI.

Running the project on newer non-LTS JDKs may produce warnings from gRPC/Netty about deprecated `sun.misc.Unsafe` memory access APIs.

These warnings are produced by the gRPC/Netty dependency stack, not by application code.

They are not currently treated as project issues.

---

### Event Storming alignment

The Event Storming model should be updated after the latest lifecycle and integration changes.

The current implementation supports:

- room hold placement
- hold confirmation
- hold release
- confirmed booking cancellation
- booked room release
- gRPC-based booking-to-inventory communication in the main runtime profile

Future Event Storming updates should explicitly show the difference between:

- cancelling a held booking
- cancelling a confirmed booking
- synchronous inventory reservation call
- future event-driven booking process

---

### API semantics

The current cancellation endpoint is:

```text
POST /api/v1/bookings/{bookingId}/cancel
```

It supports both held and confirmed bookings.

The endpoint does not physically delete a booking.

The API should continue to describe this operation as cancellation, not deletion.

Possible future improvements:

- add clearer OpenAPI descriptions
- document valid state transitions
- return more specific error codes for invalid transitions
- distinguish invalid references, business conflicts and downstream service failures

---

## Planned next steps

### v0.4.0

Remove transitional runtime pieces:

1. Remove in-memory runtime profile.
2. Remove direct Java booking-to-inventory adapters.
3. Keep booking-to-inventory communication through gRPC only.
4. Adjust integration tests to the new runtime model.

### v0.5.0

Extract Gradle modules and separately runnable applications:

1. Extract Gradle modules.
2. Add `booking-service-app`.
3. Add `inventory-service-app`.
4. Remove the single application entry point.
5. Keep one codebase, but allow separate service startup.

### v0.6.0

Add security:

1. Add JWT support.
2. Add Google OAuth2 login.
3. Associate bookings with authenticated users.
4. Keep security concerns outside the domain model.

### v0.7.0

Add outbox:

1. Add transactional outbox for booking events.
2. Publish booking lifecycle events reliably.
3. Prepare for event-driven communication.

### v0.8.0

Add saga/process manager:

1. Introduce explicit booking process state.
2. Handle distributed booking flow.
3. Add compensation and retry behavior.

### v0.9.0

Improve observability and resilience:

1. Add structured logging.
2. Add correlation IDs.
3. Add metrics.
4. Add tracing.
5. Add gRPC deadlines and retry strategy.

### v1.0.0

Prepare portfolio-ready release:

1. Clean architecture documentation.
2. Add ADRs.
3. Add diagrams.
4. Add complete local startup guide.
5. Add final README polish.
