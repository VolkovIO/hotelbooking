# Technical Debt

## Current version

`v0.2.2`

The project is currently a learning modular monolith focused on Clean Architecture, DDD tactical patterns and explicit module boundaries.

The main implemented booking flow is:

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

The project now has PostgreSQL persistence for booking and MongoDB persistence for inventory.
It is still intentionally not production-ready: transaction boundaries, cross-module consistency,
optimistic locking and idempotency are not fully addressed yet.

---

## Completed improvements

### Module boundaries

Booking and inventory are separated into explicit modules.

Booking no longer depends directly on inventory domain objects. Integration goes through booking outbound ports and inventory published application use cases.

Current booking outbound ports:

- `InventoryLookupPort`
- `InventoryReservationPort`

Current inventory published use cases:

- `InventoryQueryUseCase`
- `InventoryReservationUseCase`

The adapter between booking and inventory acts as an anti-corruption layer. It maps inventory-side application results and exceptions to booking-side contracts.

---

### Booking-inventory lookup contract

Booking no longer asks inventory several low-level questions such as:

- whether a hotel exists
- whether a room type exists
- what guest capacity the room type has

Instead, booking requests a single room type reference required for its use case.

This reduces coupling between modules and prepares the boundary for future persistence work.

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

### Transaction boundaries

Booking creation currently performs an inventory hold before saving the booking.

If inventory hold creation succeeds but booking persistence fails, an orphan inventory hold may remain.

Confirmed booking cancellation updates inventory first and then saves the booking.

If booking persistence fails after inventory is updated, booking and inventory may become inconsistent.

This is acceptable for the current in-memory learning stage, but must be addressed before real persistence.

Possible future options:

- single database transaction in a modular monolith
- optimistic locking
- outbox pattern
- saga or process manager if modules become distributed services

---

### Inventory reservation identity

The current model clears `holdId` after booking confirmation.

Confirmed booking cancellation is currently performed by:

    hotelId + roomTypeId + stayPeriod + rooms

This is simple and works for the current learning stage.

A future improvement may introduce a more explicit inventory reservation identity, for example:

    inventoryReservationId

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

Before adding real persistence, the project should decide whether inventory entities should consistently use:

- mutable entity-style methods
- immutable replacement-style methods

This should be clarified before implementing JDBC or Mongo repositories.

---

### Persistence readiness

Aggregates and repositories are currently optimized for in-memory storage.

Before adding real persistence, the project should add:

- restore or rehydration factory methods for aggregates
- repository contract tests
- persistence-specific adapter tests
- transaction boundary decisions
- optimistic locking strategy

Potential aggregates/entities requiring restore methods:

- `Booking`
- `Hotel`
- `RoomAvailability`
- `RoomHold`

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

### Idempotency

Current command operations are not idempotent.

Examples:

- confirming an already confirmed booking is rejected
- cancelling an already cancelled booking is rejected
- repeated HTTP calls may produce domain errors

This is acceptable for now.

Real distributed systems usually need safer retry behavior, for example:

- idempotency keys
- command identifiers
- request deduplication
- process state tracking

---

### Event Storming alignment

The Event Storming model should be updated after the latest lifecycle changes.

The current implementation supports:

- room hold placement
- hold confirmation
- hold release
- confirmed booking cancellation
- booked room release

Future Event Storming updates should explicitly show the difference between:

- cancelling a held booking
- cancelling a confirmed booking

---

### API semantics

The current cancellation endpoint is:

    POST /api/v1/bookings/{bookingId}/cancel

It now supports both held and confirmed bookings.

The endpoint does not physically delete a booking.

The API should continue to describe this operation as cancellation, not deletion.

Possible future improvements:

- add clearer OpenAPI descriptions
- document valid state transitions
- return more specific error codes for invalid transitions

---

### Error model

Inventory-specific exceptions should not leak through booking flows.

Booking should expose booking-level errors to API clients.

The booking-inventory ACL adapter should continue translating inventory failures into booking application exceptions while preserving the original cause for diagnostics.

Possible future improvements:

- introduce stable application error codes
- separate domain errors from integration errors
- improve error response consistency across modules

---

## Planned next steps

Recommended next steps before persistence:

1. Add application-level booking lifecycle scenario tests.
2. Update Event Storming diagram to match the current implemented flow.
3. Prepare aggregates for persistence.
4. Add repository contract tests.
5. Decide transaction strategy.
6. Add PostgreSQL persistence foundation.

Persistence should not be added before the lifecycle rules are covered by tests.