# Hotel Booking

A learning-oriented backend for hotel booking, built with Java and Spring Boot.

The project demonstrates an evolutionary architecture path:

```text
modular monolith
  -> explicit module boundaries
  -> gRPC boundary between modules
  -> separately runnable service applications
  -> security and ownership model
  -> transactional outbox
  -> Kafka integration
  -> saga/process manager
  -> observability and resilience
```

The main goal of the project is to practice Clean Architecture, DDD tactical patterns, modular design, persistence boundaries, transport contracts and the gradual transition from a modular monolith toward distributed services.

The project is intentionally not production-ready yet.

---

## Current stage: v0.6.0

Starting from `v0.5.0`, the booking service supports:

```text
Google JWT authentication
internal application user mapping
booking ownership checks
Google JWT audience validation
```

Starting from `v0.5.2`, the project also clarifies the target security model:

```text
User / Frontend
  -> Booking HTTP API
  -> Google JWT

Booking service
  -> Inventory gRPC API
  -> mTLS
```

Starting from `v0.6.0`, the booking service records lifecycle events in a transactional outbox.

```text
The current outbox implementation writes events to PostgreSQL but does not publish them yet.
Outbox polling and Kafka publication are planned for later releases.

Local development profiles must be activated explicitly.

Public inventory catalog endpoints are available without authentication so that users can browse hotels, room types and availability before login.

Booking creation and booking management require authentication.
```
---

## Project goals

This project is not just a CRUD hotel booking application.

The educational goal is to demonstrate senior-level backend topics:

```text
Clean Architecture
DDD tactical patterns
Hexagonal Architecture
module boundaries
anti-corruption layer
PostgreSQL persistence
MongoDB persistence
gRPC integration
Google JWT authentication
booking ownership
transactional outbox
Kafka integration
idempotency
mTLS service-to-service security
symbolic payment flow
notification service
saga/process manager
audit events
observability
Testcontainers and integration testing
```

---

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

  docs/
    security-model.md
    technical-debt.md
    logging-strategy.md
```

---

## Applications

### booking-service-app

`apps/booking-service-app` is the booking runtime application.

It exposes the booking HTTP API, stores booking data in PostgreSQL and calls inventory through gRPC.

Responsibilities:

```text
booking lifecycle
booking ownership
Google JWT based external user authentication
PostgreSQL booking persistence
gRPC client adapter to inventory
```

### inventory-service-app

`apps/inventory-service-app` is the inventory runtime application.

It exposes the inventory HTTP API, stores inventory data in MongoDB and exposes the inventory gRPC server.

Responsibilities:

```text
hotel catalog
room types
room availability
room holds
MongoDB inventory persistence
gRPC server adapter for booking integration
```

---

## Modules

`modules/booking` contains booking domain, booking application use cases, booking HTTP adapters, booking PostgreSQL persistence and booking gRPC client integration.

`modules/inventory` contains inventory domain, inventory application use cases, inventory HTTP adapters, inventory MongoDB persistence and inventory gRPC server adapters.

`modules/inventory-grpc-api` contains the protobuf contract and generated gRPC Java API used by both booking and inventory.

---

## Runtime architecture

Current runtime flow:

```text
Client / Swagger / Future Frontend
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

---

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

---

## Security model

The project separates external user authentication from internal service authentication.

Target model:

```text
External user access:
  User / Frontend
    -> Booking HTTP API
    -> Google JWT

Internal service access:
  Booking service
    -> Inventory gRPC API
    -> mTLS
```

Google JWT is used for user identity and booking ownership.

mTLS is the target model for booking-to-inventory gRPC communication.

More details:

```text
docs/security-model.md
```

---

## Public browsing before login

A user should be able to browse hotels, room types and availability before logging in.

Public inventory catalog endpoints:

```text
GET /api/v1/hotels
GET /api/v1/hotels/{hotelId}
GET /api/v1/hotels/{hotelId}/room-types/{roomTypeId}/availability
```

Booking creation requires authentication:

```text
POST /api/v1/bookings
```

Intended user journey:

```text
browse hotels and availability anonymously
  -> choose room type and stay period
  -> login with Google
  -> create booking
  -> manage own booking
```

---

## Inventory admin API

Inventory administrative endpoints are intended for data setup and management.

Examples:

```text
POST /api/v1/admin/hotels
POST /api/v1/admin/hotels/{hotelId}/room-types
POST /api/v1/admin/hotels/{hotelId}/room-types/{roomTypeId}/availability/initialization
PUT  /api/v1/admin/hotels/{hotelId}/room-types/{roomTypeId}/availability/capacity
```

In local development, the `security-dev` profile provides a mock admin user with:

```text
ROLE_USER
ROLE_ADMIN
```

This is not a production-grade admin authentication model.

---

## Booking to inventory communication

Booking-to-inventory communication uses gRPC.

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

---

## Current API behavior

The current API supports:

```text
public hotel catalog browsing
public room availability lookup
admin hotel registration
admin room type registration
admin availability setup
booking creation
booking confirmation
booking cancellation
booking details lookup
booking ownership checks
```

Booking cancellation behavior:

```text
if the booking is ON_HOLD
  -> cancellation releases the inventory hold

if the booking is CONFIRMED
  -> cancellation releases booked inventory rooms

cancelled bookings remain stored with status CANCELLED
```

---

## Running locally

Docker Desktop must be running, and ports `5432`, `27017`, `8080`, `8081` and `9090` must be free.

Start PostgreSQL and MongoDB:

```bash
docker compose up -d
```

Start inventory service in one terminal:

```bash
./gradlew :apps:inventory-service-app:bootRun --args="--spring.profiles.active=local"
```

On Windows:

```bash
gradlew.bat :apps:inventory-service-app:bootRun --args="--spring.profiles.active=local"
```

Inventory service endpoints:

```text
HTTP API:  http://localhost:8081
Swagger:   http://localhost:8081/swagger-ui/index.html
gRPC API:  localhost:9090
```

Start booking service in another terminal:

```bash
./gradlew :apps:booking-service-app:bootRun --args="--spring.profiles.active=local"
```

On Windows:

```bash
gradlew.bat :apps:booking-service-app:bootRun --args="--spring.profiles.active=local"
```

Booking service endpoints:

```text
HTTP API:  http://localhost:8080
Swagger:   http://localhost:8080/swagger-ui/index.html
```

To create a booking through Swagger, start `inventory-service-app` first, then start `booking-service-app`.

---

## Local profiles

Starting from `v0.5.2`, development profiles are activated explicitly through the `local` profile.

Booking local profile expands to:

```text
booking-postgres
inventory-grpc-client
security-dev
```

Inventory local profile expands to:

```text
inventory-mongo
inventory-grpc-server
security-dev
```

This avoids silently starting services in development security mode by default.

---

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

After this, use the inventory Swagger UI to inspect public hotel catalog endpoints and the booking Swagger UI to create bookings.

---

## Swagger

Inventory Swagger:

```text
http://localhost:8081/swagger-ui/index.html
```

Booking Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

Swagger remains part of the project even after a frontend or BFF is introduced.

The future frontend is intended for demonstration and portfolio presentation.
Swagger is intended for API exploration and manual testing.

---

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

---

## Recommended Java version

Java 21 LTS is recommended.

The project may also run on newer JDKs, but newer non-LTS JDKs can produce warnings from gRPC/Netty about deprecated `sun.misc.Unsafe` memory access APIs.

---

## Current limitations

This project is still a learning system.

Known limitations:

```text
no frontend login flow yet
no service-to-service mTLS yet
no distributed transaction handling
no outbox yet
no Kafka integration yet
no saga/process manager yet
no payment service yet
no notification service yet
no audit service yet
no production-grade retry/idempotency model
no centralized observability yet
service-level integration tests need to be restored and extended
```

More details:

```text
docs/technical-debt.md
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

---

## Portfolio positioning

The project can be presented as:

```text
Educational distributed booking platform demonstrating Clean Architecture, DDD,
transactional outbox, Kafka-based integration, mTLS service-to-service security,
saga orchestration, idempotency, observability and production-oriented testing.
```

The focus is not only on Spring Boot, but on architectural trade-offs:

```text
service boundaries
data ownership
consistency
transactions
security
event-driven integration
failure handling
observability
testing strategy
```
