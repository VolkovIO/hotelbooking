# Hotel Booking

A modular monolith backend for hotel booking, built with Java and Spring Boot.

This project is being developed as a learning-oriented implementation of Clean Architecture, DDD, and an evolutionary path from a modular monolith to microservices.

## Current stage: v0.2.1

The project is a learning modular monolith for hotel booking.

Current focus:

- Clean Architecture boundaries
- DDD-style aggregates and value objects
- explicit `booking` and `inventory` modules
- booking-inventory integration through published application use cases
- in-memory infrastructure adapters
- room availability, room holds, booking confirmation and cancellation flows

The project is intentionally not production-ready yet.
Persistence, transaction boundaries, outbox/events and stronger consistency guarantees are planned as future improvements.

## Booking lifecycle

The booking module currently supports the following lifecycle:

| State | Meaning |
|---|---|
| `ON_HOLD` | Booking was created and inventory rooms are temporarily held |
| `CONFIRMED` | Booking was confirmed and held rooms were converted to booked rooms |
| `CANCELLED` | Booking was cancelled |

Current supported transitions:

| Operation | From | To | Inventory effect |
|---|---|---|---|
| Create booking | - | `ON_HOLD` | Places an inventory hold |
| Confirm booking | `ON_HOLD` | `CONFIRMED` | Converts held rooms to booked rooms |
| Cancel held booking | `ON_HOLD` | `CANCELLED` | Releases the inventory hold |
| Cancel confirmed booking | `CONFIRMED` | `CANCELLED` | Releases booked inventory rooms |

A booking is not physically deleted when it is cancelled.
Cancellation is represented by the `CANCELLED` status.

## Module boundaries

The project is organized as a modular monolith.

Main modules:

- `booking` — owns the booking lifecycle
- `inventory` — owns hotels, room types, room availability and room holds
- `common/api` — shared HTTP error handling

The booking module does not depend on inventory domain objects directly.

Booking integrates with inventory through booking outbound ports:

- `InventoryLookupPort`
- `InventoryReservationPort`

Inventory exposes published application use cases:

- `InventoryQueryUseCase`
- `InventoryReservationUseCase`

The adapter between booking and inventory acts as an anti-corruption layer.
It maps inventory-side application results and exceptions to booking-side contracts.

## Current API behavior

The current API supports:

- registering hotels and room types
- configuring room availability
- querying room availability
- creating bookings
- confirming bookings
- cancelling held bookings
- cancelling confirmed bookings
- retrieving booking details

Booking cancellation behavior:

- if the booking is `ON_HOLD`, cancellation releases the inventory hold
- if the booking is `CONFIRMED`, cancellation releases booked inventory rooms
- cancelled bookings remain stored with status `CANCELLED`


## Running with MongoDB inventory persistence

Start MongoDB:
```bash
  docker compose up -d
```

Run the application with booking in-memory persistence and inventory MongoDB persistence:
```bash
  ./gradlew bootRun --args='--spring.profiles.active=local-mongo'
```

## Initializing demo inventory data

Demo inventory data is stored in: docker/mongo/init/demo-data.js

The script creates fixed demo hotels, room types and room availability for: 2030-06-01 .. 2030-06-30

Initialize demo data from Git Bash:
```bash  
  docker compose exec -T mongo mongosh "mongodb://localhost:27017/hotelbooking" < docker/mongo/init/demo-data.js
```

Or Windows PowerShell:
```bash 
  Get-Content docker/mongo/init/demo-data.js | docker compose exec -T mongo mongosh "mongodb://localhost:27017/hotelbooking"
```
