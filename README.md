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
  -> outbox polling publisher
  -> Kafka integration
  -> saga/process manager
  -> observability and resilience
```

The main goal of the project is to practice Clean Architecture, DDD tactical patterns, modular design, persistence boundaries, transport contracts and the gradual transition from a modular monolith toward distributed services.

The project is intentionally not production-ready yet.

---

## Current stage: v0.6.2

Starting from `v0.5.0`, the booking service supports:

```text
Google JWT authentication
internal application user mapping
booking ownership checks
Google JWT audience validation
```

Starting from `v0.5.2`, the project clarifies the target security model:

```text
User / Frontend
  -> Booking HTTP API
  -> Google JWT

Booking service
  -> Inventory gRPC API
  -> mTLS
```

Starting from `v0.6.0`, the booking service records lifecycle events in a transactional outbox.

Starting from `v0.6.1`, the booking service includes a scheduled outbox polling publisher.

The current publisher uses a logging adapter and does not send events to Kafka yet.

Kafka publication is planned for `v0.7.0`.

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
outbox polling and retry handling
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
    inventory-service-app/

  modules/
    booking/
    inventory/
    inventory-grpc-api/

  docs/
    security-model.md
    technical-debt.md
    outbox.md
    logging-strategy.md
```

---

## Applications

### booking-service-app

`apps/booking-service-app` is the booking runtime application.

It exposes the booking HTTP API, stores booking data in PostgreSQL, writes booking events to the outbox and calls inventory through gRPC.

Responsibilities:

```text
booking lifecycle
booking ownership
Google JWT based external user authentication
PostgreSQL booking persistence
transactional outbox
outbox polling publisher
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

Booking state and outbox flow:

```text
Booking application use case
  -> booking state change
  -> BookingStateChangePersistenceService
  -> PostgreSQL booking table
  -> PostgreSQL booking_outbox table
```

Outbox polling flow:

```text
BookingOutboxScheduler
  -> BookingOutboxPollingService
  -> claim NEW outbox rows as PROCESSING
  -> LoggingBookingOutboxEventPublisher
  -> mark rows as PUBLISHED / NEW retry / FAILED
```

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

| Operation | From | To | Inventory effect | Outbox event |
|---|---|---|---|---|
| Create booking | - | `ON_HOLD` | Places an inventory hold | `BookingPlacedOnHold` |
| Confirm booking | `ON_HOLD` | `CONFIRMED` | Converts held rooms to booked rooms | `BookingConfirmed` |
| Cancel held booking | `ON_HOLD` | `CANCELLED` | Releases the inventory hold | `BookingCancelled` |
| Cancel confirmed booking | `CONFIRMED` | `CANCELLED` | Releases booked inventory rooms | `BookingCancelled` |

A booking is not physically deleted when it is cancelled.
Cancellation is represented by the `CANCELLED` status.

---

## Transactional outbox

The booking service records booking lifecycle events in a PostgreSQL outbox table.

Current event types:

```text
BookingPlacedOnHold
BookingConfirmed
BookingCancelled
```

Current outbox statuses:

```text
NEW
PROCESSING
PUBLISHED
FAILED
```

The local transaction guarantees:

```text
booking state change
  + booking outbox event insert
```

The current publisher is a scheduled polling publisher with a logging adapter.

This means events are processed like this:

```text
NEW
  -> PROCESSING
  -> PUBLISHED
```

On failure:

```text
PROCESSING
  -> NEW with next_attempt_at for retry
  -> or FAILED when max attempts are reached
```

Kafka is not used yet.

More details:

```text
docs/outbox.md
```

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

Inventory service endpoints:

```text
HTTP API:  http://localhost:8081
Swagger:   http://localhost:8081/swagger-ui/index.html
gRPC API:  localhost:9090
```

---

## Local profiles

Booking local profile expands to:

```text
booking-postgres
inventory-grpc-client
security-dev
outbox-publisher
outbox-logging
```

Inventory local profile expands to:

```text
inventory-mongo
inventory-grpc-server
security-dev
```

---

## Verifying outbox processing

After creating, confirming or cancelling a booking, check the outbox table:

```sql
select id, event_type, status, attempts, published_at, last_error
from booking_outbox
order by created_at desc;
```

Expected result in local mode:

```text
status = PUBLISHED
published_at is not null
```

The booking service logs should include messages from the logging outbox publisher.

---

## Build and quality checks

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

## Current limitations

This project is still a learning system.

Known limitations:

```text
no frontend login flow yet
no service-to-service mTLS yet
no distributed transaction handling
no Kafka integration yet
no saga/process manager yet
no payment service yet
no notification service yet
no audit service yet
no production-grade idempotency model yet
no consumer inbox pattern yet
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
