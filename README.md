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

## Current stage: v0.4.0

Starting from `v0.4.0`, the project is a **Gradle multi-project build** with two separately runnable Spring Boot applications:

- `booking-service-app`
- `inventory-service-app`

The codebase is still one repository, but the runtime is no longer a single application.

Booking and inventory are now started as separate applications and communicate through the gRPC boundary introduced in `v0.3.0`.

Current focus:

- Clean Architecture boundaries
- DDD-style aggregates and value objects
- explicit `booking` and `inventory` modules
- separate Spring Boot applications for booking and inventory
- PostgreSQL persistence for booking
- MongoDB persistence for inventory
- gRPC contract between booking and inventory
- booking gRPC client adapter
- inventory gRPC server adapter
- room availability, room holds, booking confirmation and cancellation flows

The project is intentionally not production-ready yet.

Transaction boundaries, cross-service consistency, outbox/events, retries, idempotency, security and observability are planned as future improvements.

## Project structure

```text
hotelbooking/
  settings.gradle
  build.gradle

  apps/
    booking-service-app/
      build.gradle
      src/main/java
      src/main/resources

    inventory-service-app/
      build.gradle
      src/main/java
      src/main/resources

  modules/
    booking/
      build.gradle
      src/main/java
      src/main/resources
      src/test/java

    inventory/
      build.gradle
      src/main/java
      src/test/java

    inventory-grpc-api/
      build.gradle
      src/main/proto
```

### Applications

`apps/booking-service-app` is the booking runtime application.

It exposes the booking HTTP API, stores booking data in PostgreSQL and calls inventory through gRPC.

`apps/inventory-service-app` is the inventory runtime application.

It exposes the inventory HTTP API, stores inventory data in MongoDB and exposes the inventory gRPC server.

### Modules

`modules/booking` contains booking domain, booking application use cases, booking HTTP adapters, booking PostgreSQL persistence and booking gRPC client integration.

`modules/inventory` contains inventory domain, inventory application use cases, inventory HTTP adapters, inventory MongoDB persistence and inventory gRPC server adapters.

`modules/inventory-grpc-api` contains the protobuf contract and generated gRPC Java API used by both booking and inventory.

## Runtime architecture

Current runtime flow:

```text
Client / Swagger
  -> booking-service-app HTTP API :8080
  -> BookingController
  -> Booking application use case
  -> InventoryLookupPort / InventoryReservationPort
  -> booking gRPC client adapter
  -> inventory-service-app gRPC API :9090
  -> inventory gRPC server adapter
  -> Inventory application use case
  -> MongoDB
```

Booking service:

```text
booking-service-app
  -> modules:booking
  -> modules:inventory-grpc-api
  -> PostgreSQL
  -> gRPC client to inventory-service-app
```

Inventory service:

```text
inventory-service-app
  -> modules:inventory
  -> modules:inventory-grpc-api
  -> MongoDB
  -> gRPC server
```

The booking service must not depend on the inventory domain or inventory application code directly.

Allowed dependency:

```text
booking -> inventory-grpc-api
```

Forbidden dependency:

```text
booking -> inventory domain/application
```

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

The project is organized around explicit business modules.

Main modules:

- `booking` — owns the booking lifecycle
- `inventory` — owns hotels, room types, room availability and room holds
- `inventory-grpc-api` — owns the public gRPC contract exposed by inventory

The booking module does not depend on inventory domain objects directly.

Booking integrates with inventory through booking outbound ports:

- `InventoryLookupPort`
- `InventoryReservationPort`

In the separated runtime these ports are implemented by gRPC client adapters.

Inventory exposes gRPC server adapters that call inventory application use cases:

- `InventoryQueryUseCase`
- `InventoryReservationUseCase`

The adapter between booking and inventory acts as an anti-corruption layer.

## Booking to inventory communication

Starting from `v0.3.0`, booking-to-inventory communication uses gRPC.

Starting from `v0.4.0`, booking and inventory are no longer started from a single Spring Boot application. They are separate Spring Boot applications in the same repository.

Runtime communication:

```text
booking-service-app
  -> gRPC client
  -> inventory-service-app
```

The gRPC contract is located in:

```text
modules/inventory-grpc-api/src/main/proto/inventory/v1/inventory_service.proto
```

Generated Java/gRPC classes are produced by Gradle during the build.

Generate protobuf sources manually:

```bash
./gradlew :modules:inventory-grpc-api:generateProto
```

Generated sources are located under:

```text
modules/inventory-grpc-api/build/generated/sources/proto
```

They are not committed to Git.

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

## Running locally

Docker Desktop must be running, and ports `5432`, `27017`, `8080`, `8081` and `9090` must be free.

Start PostgreSQL and MongoDB:

```bash
docker compose up -d
```

Start inventory service in one terminal:

```bash
./gradlew :apps:inventory-service-app:bootRun
```

On Windows:

```bash
gradlew.bat :apps:inventory-service-app:bootRun
```

Inventory service endpoints:

```text
HTTP API:  http://localhost:8081
Swagger:   http://localhost:8081/swagger-ui/index.html
gRPC API:  localhost:9090
```

Start booking service in another terminal:

```bash
./gradlew :apps:booking-service-app:bootRun
```

On Windows:

```bash
gradlew.bat :apps:booking-service-app:bootRun
```

Booking service endpoints:

```text
HTTP API:  http://localhost:8080
Swagger:   http://localhost:8080/swagger-ui/index.html
```

To create a booking through Swagger, start `inventory-service-app` first, then start `booking-service-app`.

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

After this, use the booking Swagger UI to create a booking for the demo hotel and room type.

## Build and quality checks

Show Gradle projects:

```bash
./gradlew projects
```

Run tests:

```bash
./gradlew test
```

Run full verification checks:

```bash
./gradlew clean check
```

On Windows:

```bash
gradlew.bat clean check
```

`check` runs tests and static analysis tasks such as Checkstyle, PMD and SpotBugs.

Format code:

```bash
./gradlew spotlessApply
```

Build service jars:

```bash
./gradlew :apps:inventory-service-app:bootJar
./gradlew :apps:booking-service-app:bootJar
```

## Recommended Java version

Java 21 LTS is recommended.

The project may also run on newer JDKs, but newer non-LTS JDKs can produce warnings from gRPC/Netty about deprecated `sun.misc.Unsafe` memory access APIs.

## Current limitations

This project is still a learning system.

Known limitations:

- no authentication yet
- no user ownership for bookings yet
- no distributed transaction handling
- no outbox yet
- no saga/process manager yet
- no production-grade retry/idempotency model
- no centralized observability yet
- service-level integration tests need to be restored after application extraction


