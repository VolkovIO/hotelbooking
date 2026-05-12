# Hotel Booking — Senior Java Learning Project

Hotel Booking is an educational backend project for practicing production-style Java development.

The main goal of the project is not to build a commercial booking product, but to gradually study and demonstrate senior-level backend engineering topics:

- Clean Architecture / Hexagonal Architecture
- Domain-Driven Design basics
- modular monolith decomposition and transition to services
- Spring Boot service design
- PostgreSQL and MongoDB persistence
- Kafka-based event-driven integration
- gRPC service-to-service communication
- transactional outbox pattern
- idempotency and retry handling
- Docker Compose based local infrastructure
- orchestration saga and compensation
- comparison of handmade orchestration and Spring Statemachine
- service-level integration testing with Testcontainers
- concurrency safety for finite inventory
- GitHub Actions CI

The project is intentionally developed step by step. Each milestone adds one or several architectural concepts and keeps the implementation understandable for learning and interview discussion.

## Current milestone

Current version:

```text
v0.12.0 — Integration tests, concurrency safety, and CI
```

This version adds the first service-level integration tests for critical concurrency scenarios.

The main focus is last room contention:

```text
many clients try to book the same last available room concurrently
```

Expected behavior:

```text
exactly one client wins
no over-holding is possible
only one booking is confirmed
competing bookings are rejected or failed safely
```

This milestone also adds GitHub Actions CI so that Gradle checks run automatically for pull requests and pushes to `master`.

## Architecture focus

The project is organized around Clean Architecture / Hexagonal Architecture ideas.

Typical module structure:

```text
adapter/in     -> REST controllers, Kafka consumers, schedulers
adapter/out    -> PostgreSQL, MongoDB, Kafka, gRPC, HTTP clients
application    -> use cases, ports, commands, queries, orchestration
application/port/in
application/port/out
domain         -> aggregates, value objects, invariants
```

Main principles used in the project:

- domain model does not depend on Spring
- application layer depends on ports, not adapters
- adapters implement infrastructure details
- business state changes are explicit
- integration events are published through outbox where needed
- external service calls are not wrapped into local database transactions
- critical persistence-boundary behavior is covered by integration tests

## Services and modules

The project currently contains several service applications and modules.

### Booking service

Responsible for:

- creating bookings
- holding and confirming inventory
- coordinating payment during booking creation
- publishing booking lifecycle events through outbox
- exposing the main booking saga API
- exposing the Spring Statemachine saga prototype API behind a profile

### Inventory service

Responsible for:

- hotel and room type inventory
- placing temporary holds
- confirming holds
- releasing holds
- cancelling confirmed reservations
- protecting finite room availability under concurrent hold attempts

Booking-service communicates with inventory-service through gRPC in the normal local application flow.

### Payment service

Responsible for:

- payment authorization
- payment approval
- payment cancellation
- publishing payment events through outbox
- fake payment provider for local testing

The fake payment provider supports payment decline scenarios for testing saga compensation.

### Notification service

Responsible for:

- consuming booking events from Kafka
- creating notification records
- sending notifications through a logging sender adapter

Real Telegram and Max sender adapters are intentionally not implemented yet. For this educational project, logging sender is enough to verify the full flow.

## Local infrastructure

The project uses Docker Compose for local infrastructure.

Typical local components:

- PostgreSQL
- MongoDB
- Kafka
- Kafka UI

PostgreSQL local setup uses one PostgreSQL container with separate logical databases:

```text
hotelbooking          -> booking-service
hotelbooking_payment  -> payment-service
```

MongoDB local setup uses separate logical databases for services that need Mongo persistence.

## Booking saga overview

The booking saga coordinates booking, inventory, payment, Kafka events, and notifications.

Happy path:

```text
create booking
place inventory hold
authorize payment
confirm inventory hold
confirm booking
approve payment
publish BookingConfirmed
send notification
```

Payment declined path:

```text
create booking
place inventory hold
authorize payment -> DECLINED
release inventory hold
cancel booking
publish BookingCancelled
send notification
```

The handmade saga is implemented as a process manager with durable saga state in PostgreSQL.

Important saga concepts covered by the project:

- durable saga state
- current step persistence
- compensation
- retry scheduling
- transactional outbox integration
- event-driven notification after booking events

More details are documented in:

```text
docs/booking-saga.md
docs/workflow-engine-comparison.md
```

## Integration testing and concurrency safety

Version `v0.12.0` adds automated tests for last room contention.

### Inventory-level contention

Test:

```text
InventoryLastRoomContentionIntegrationTest
```

Location:

```text
apps/inventory-service-app/src/test/java/com/example/hotelbooking/inventoryservice/InventoryLastRoomContentionIntegrationTest.java
```

The test uses real MongoDB through Testcontainers and verifies:

```text
20 concurrent hold attempts
1 available room
exactly 1 successful hold
heldRooms = 1
availableRooms = 0
```

This test protects the inventory invariant:

```text
successful holds must not exceed available rooms
```

### Atomic inventory hold reservation

Inventory hold placement now uses atomic conditional MongoDB updates instead of read-check-save updates.

Conceptually:

```text
increment heldRooms
only if totalRooms - heldRooms - bookedRooms >= requestedRooms
```

This prevents two concurrent clients from placing holds on the same last available room.

### Booking-level saga contention

Test:

```text
BookingSagaContentionIntegrationTest
```

Location:

```text
apps/booking-service-app/src/test/java/com/example/hotelbooking/bookingservice/BookingSagaContentionIntegrationTest.java
```

The test uses real booking-service Spring context and PostgreSQL through Testcontainers.

Inventory and payment are controlled test doubles. This keeps the test focused on booking saga behavior.

Expected result:

```text
completed sagas = 1
failed sagas = 19
confirmed bookings = 1
rejected bookings = 19
payment authorization calls = 1
payment approval calls = 1
payment cancellation calls = 0
```

More details are documented in:

```text
docs/integration-testing.md
```

## GitHub Actions CI

The project contains a CI workflow:

```text
.github/workflows/ci.yml
```

The workflow runs on:

```text
pull requests to master
pushes to master
```

Main CI command:

```bash
./gradlew check --no-daemon --stacktrace
```

The workflow uses Java 21 and Gradle caching.

If checks fail, test/static-analysis reports are uploaded as workflow artifacts.

## Running checks

Common checks:

```bash
./gradlew spotlessApply
./gradlew check
```

Module-focused checks:

```bash
./gradlew :modules:booking:check
./gradlew :modules:inventory:check
./gradlew :apps:booking-service-app:check
./gradlew :apps:inventory-service-app:check
```

Focused integration tests:

```bash
./gradlew :apps:inventory-service-app:integrationTest --tests "*InventoryLastRoomContentionIntegrationTest"
./gradlew :apps:booking-service-app:integrationTest --tests "*BookingSagaContentionIntegrationTest"
```

Before pull requests, run:

```bash
./gradlew clean check
```

## Running the booking service

Example local run:

```bash
./gradlew :apps:booking-service-app:bootRun --args="--spring.profiles.active=local-kafka"
```

To enable the Spring Statemachine prototype endpoint:

```bash
./gradlew :apps:booking-service-app:bootRun --args="--spring.profiles.active=local-kafka,booking-saga-springstatemachine-prototype"
```

## Manual saga verification

Happy path request:

```http
POST /api/v1/bookings/saga
```

Use a payment amount below the fake provider decline threshold.

Expected result:

```text
Booking       -> CONFIRMED
BookingSaga   -> COMPLETED
Payment       -> APPROVED
Kafka event   -> BookingConfirmed
Notification  -> confirmation notification sent by logging adapter
```

Payment declined request:

Use a payment amount above the fake provider decline threshold.

Expected result:

```text
Booking       -> CANCELLED
BookingSaga   -> COMPENSATED
Payment       -> DECLINED
Kafka event   -> BookingCancelled
Notification  -> cancellation notification sent by logging adapter
```

The same scenarios can be tested through the Spring Statemachine prototype endpoint when the prototype profile is enabled.

## Development philosophy

The project is intentionally built in small milestones.

The priority is to understand and demonstrate architectural decisions, not to add as many features as possible.

Examples of conscious trade-offs:

- notification sender adapters currently log messages instead of calling real Telegram or Max APIs
- payment provider is fake but supports success and decline scenarios
- cancellation after already approved payment does not implement refund yet
- automatic inventory hold expiration is documented as future hardening
- Spring Statemachine is introduced as a prototype, not as a replacement for the main flow
- Temporal is compared in documentation, not added as infrastructure yet
- booking-level contention test uses inventory/payment doubles instead of full multi-service e2e infrastructure

This keeps the project understandable and useful for interview discussion.

## Roadmap

Completed milestones include:

- booking service foundation
- inventory service foundation
- notification service and Kafka consumption
- payment service and payment outbox
- booking saga orchestration
- saga retry and compensation
- saga action extraction
- Spring Statemachine prototype for orchestration comparison
- inventory concurrency safety with atomic hold reservation
- service-level integration tests with Testcontainers
- GitHub Actions CI

Possible future milestones:

- audit/event timeline service with correlation and causation IDs
- observability with structured logs, metrics, tracing, and optional centralized logging
- load testing with k6
- stronger idempotency and reconciliation for unknown outcomes
- cancellation and refund process for already approved bookings
- automatic inventory hold expiration
- Temporal-based workflow prototype in a separate branch or milestone
- richer integration, contract, and full e2e tests

## Interview discussion points

This project can be used to discuss:

- how to structure Spring Boot services with Clean Architecture
- how to model domain invariants and value objects
- when to use PostgreSQL vs MongoDB
- how transactional outbox helps with reliable event publication
- how Kafka integrates services asynchronously
- why gRPC can be useful for internal synchronous service calls
- how saga orchestration differs from distributed transactions
- how compensation differs from rollback
- why payment authorization and approval are separate
- why inventory hold and confirmation are separate
- how handmade process manager compares with Spring Statemachine and Temporal
- why read-check-save is unsafe under high contention
- how atomic conditional updates protect finite inventory
- how Testcontainers helps verify real persistence behavior
- why CI is important for regression protection
