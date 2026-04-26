# Technical Debt

## Current version

v0.2.0

## Completed before v0.2.0

- Booking and inventory were separated into explicit modules.
- Booking no longer depends directly on inventory domain objects.
- Inventory exposes published application use cases for booking integration.
- Booking uses its own outbound ports for inventory lookup and reservation.
- Basic domain invariants were added for booking status transitions, guest count and availability ranges.

## Known technical debt

### Transaction boundaries

Booking creation currently performs an inventory hold before saving the booking.
If booking persistence fails after the hold is placed, an orphan hold may remain.

Inventory hold placement updates room availability and then saves the room hold.
If saving the hold fails after availability was updated, availability may become inconsistent.

This is acceptable for the current in-memory learning stage, but must be addressed before real persistence.

Possible future options:
- single database transaction in a modular monolith
- optimistic locking
- outbox pattern
- saga/process manager if modules become distributed services

### Booking-inventory error translation

The booking module should not expose inventory-specific exceptions through its API.
The ACL adapter should translate inventory reservation failures into booking-level application exceptions.

### Published query contract

Booking currently performs several inventory lookup calls.
A better contract would be a single room type reference query returning a small snapshot required by booking.

Example:

```java
RoomTypeReference {
  hotelId
  roomTypeId
  guestCapacity
}