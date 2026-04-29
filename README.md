# Hotel Booking

A modular monolith backend for hotel booking, built with Java and Spring Boot.

This project is being developed as a learning-oriented implementation of Clean Architecture, DDD, and an evolutionary path from a modular monolith to microservices.

## Current stage: v0.2.2

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



## Runtime profiles

The application supports several local runtime profile groups.

These profiles are used to switch persistence adapters without changing application or domain code.

| Profile group | Booking persistence | Inventory persistence | Purpose |
|---|---|---|---|
| `local-in-memory` | In-memory | In-memory | Fast local/demo mode without external infrastructure |
| `local-mongo` | In-memory | MongoDB | Testing MongoDB persistence for the inventory module |
| `local-postgres-mongo` | PostgreSQL | MongoDB | Current full persistence mode |

Profile groups are configured in `application.yaml`:

```yaml
spring:
  profiles:
    group:
      local-in-memory:
        - booking-in-memory
        - inventory-in-memory
      local-mongo:
        - booking-in-memory
        - inventory-mongo
      local-postgres-mongo:
        - booking-postgres
        - inventory-mongo
```

Start MongoDB + Postgres in docker: 
```bash 
  docker compose up -d
```

Run with a profile group: 

`./gradlew bootRun --args='--spring.profiles.active=local-postgres-mongo'`


## Initializing demo inventory MongoDB data

Demo inventory data is stored in: docker/mongo/init/demo-data.js

The script creates fixed demo hotels, room types and room availability for: 2030-06-01 .. 2030-06-30

Initialize demo data from Git Bash:

```bash
  docker compose exec -T mongo mongosh "mongodb://localhost:27017/hotelbooking" < docker/mongo/init/demo-data.js
```
