# Hotel Booking

A learning-oriented backend for hotel booking, built with Java and Spring Boot.

The project demonstrates an evolutionary architecture path:

```text
modular monolith
  -> explicit module boundaries
  -> gRPC boundary between modules
  -> separately runnable service applications
  -> distributed consistency patterns
```

The main goal of the project is to practice Clean Architecture, DDD tactical patterns, modular design, persistence boundaries, transport contracts and the gradual transition from a modular monolith toward microservices.

## Current stage: v0.3.0

The project is still deployed as a single Spring Boot application.

Starting from `v0.3.0`, the main PostgreSQL + MongoDB runtime profile routes booking-to-inventory communication through gRPC.

This means the application is still one runtime process, but the boundary between `booking` and `inventory` is now expressed through an explicit transport contract.

Current focus:

- Clean Architecture boundaries
- DDD-style aggregates and value objects
- explicit `booking` and `inventory` modules
- PostgreSQL persistence for booking
- MongoDB persistence for inventory
- gRPC contract between booking and inventory
- booking gRPC client adapter
- inventory gRPC server adapter
- room availability, room holds, booking confirmation and cancellation flows

The project is intentionally not production-ready yet.

Transaction boundaries, cross-module consistency, outbox/events, retries, idempotency and observability are planned as future improvements.

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

The booking module does not depend on inventory domain objects directly.

Booking integrates with inventory through booking outbound ports:

- `InventoryLookupPort`
- `InventoryReservationPort`

Inventory exposes published application use cases:

- `InventoryQueryUseCase`
- `InventoryReservationUseCase`

The adapter between booking and inventory acts as an anti-corruption layer.

In the main PostgreSQL + MongoDB profile, this anti-corruption layer is implemented through gRPC client adapters on the booking side and gRPC server adapters on the inventory side.

## Booking to inventory communication

Starting from `v0.3.0`, the main PostgreSQL + MongoDB profile uses gRPC for booking-to-inventory communication.

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

The application is still one Spring Boot runtime at this stage.

The gRPC boundary is introduced before splitting the runtime into separate applications. This allows the communication contract to be validated while the system is still easy to run and debug.

Temporary compatibility:

- `inventory-direct-client` keeps the old direct Java in-process adapter available.
- `inventory-grpc-client` enables the booking gRPC client adapter.
- `inventory-grpc-server` enables the inventory gRPC server adapter.

The direct Java inventory client is temporary and will be removed in the next architecture step.

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

These profiles are used to switch persistence adapters and integration adapters without changing application or domain code.

| Profile group | Booking persistence | Inventory persistence | Booking -> Inventory integration | Purpose |
|---|---|---|---|---|
| `local-in-memory` | In-memory | In-memory | Direct Java in-process client | Fast local/demo mode without external infrastructure |
| `local-mongo` | In-memory | MongoDB | Direct Java in-process client | Testing MongoDB persistence for the inventory module |
| `local-postgres-mongo` | PostgreSQL | MongoDB | gRPC | Current main persistence and integration mode |

Profile groups are configured in `application.yaml`:

```yaml
spring:
  profiles:
    group:
      local-in-memory:
        - booking-in-memory
        - inventory-in-memory
        - demo-in-memory
        - inventory-direct-client
      local-mongo:
        - booking-in-memory
        - inventory-mongo
        - inventory-direct-client
      local-postgres-mongo:
        - booking-postgres
        - inventory-mongo
        - inventory-grpc-server
        - inventory-grpc-client
```

The main profile for the current stage is:

```text
local-postgres-mongo
```

It starts:

- booking persistence through PostgreSQL
- inventory persistence through MongoDB
- inventory gRPC server on port `9090`
- booking gRPC client connected to `localhost:9090`

## Running locally

Docker Desktop must be running, and ports 5432 and 27017 must be free.

Start MongoDB and PostgreSQL in Docker:

```bash
  docker compose up -d
```

Run the application with the main profile:

`./gradlew bootRun --args='--spring.profiles.active=local-postgres-mongo'`


Swagger UI is available after startup at:

```text
http://localhost:8080/swagger-ui/index.html
```

## Initializing demo inventory MongoDB data

Demo inventory data is stored in:

```text
docker/mongo/init/demo-data.js
```

The script creates fixed demo hotels, room types and room availability for:

```text
2030-06-01 .. 2030-06-30
```

Initialize demo data from Git Bash:

```bash
  docker compose exec -T mongo mongosh "mongodb://localhost:27017/hotelbooking" < docker/mongo/init/demo-data.js
```

## gRPC contract

The inventory gRPC contract is located in:

```text
src/main/proto/inventory/v1/inventory_service.proto
```

Generated Java/gRPC classes are produced by Gradle during the build.

Generate protobuf sources manually:

```bash
  ./gradlew generateProto
```

Generated sources are located under:

```text
build/generated/sources/proto
```

They are not committed to Git.

## Quality checks

Run tests:

```bash
  ./gradlew test
```

Run verification without integration test:

```bash
  ./gradlew clean check -x integrationTest
```

Run full verification checks:

```bash
  ./gradlew clean check
```

`check` runs tests and static analysis tasks such as Checkstyle, PMD and SpotBugs.

