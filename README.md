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

The project is intentionally developed step by step. Each milestone adds one or several architectural concepts and keeps the implementation understandable for learning and interview discussion.

## Current milestone

Current version:

```text
v0.11.0 — Saga orchestration comparison
```

This version keeps the handmade booking saga as the main production-like flow and adds an experimental Spring Statemachine-based prototype for comparison.

The main booking saga endpoint remains:

```http
POST /api/v1/bookings/saga
```

The Spring Statemachine prototype endpoint is available only with profile:

```text
booking-saga-springstatemachine-prototype
```

Prototype endpoint:

```http
POST /api/v1/bookings/saga-statemachine
```

Both implementations reuse the same extracted saga action classes. This allows comparing orchestration approaches without duplicating the business steps.

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

Booking-service communicates with inventory-service through gRPC.

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

## Handmade saga vs Spring Statemachine prototype

The project contains two booking saga orchestration styles.

| Implementation | Endpoint | Purpose |
|---|---|---|
| Handmade process manager | `POST /api/v1/bookings/saga` | Main production-like flow |
| Spring Statemachine prototype | `POST /api/v1/bookings/saga-statemachine` | Learning and comparison |

The handmade process manager owns:

- retry loop
- current step execution
- compensation decision
- durable state handling

The Spring Statemachine prototype demonstrates:

- states
- transitions
- guards
- choice state
- action reuse

Temporal is currently documented as a production-grade alternative, but is not implemented in code in this milestone because it requires separate workflow infrastructure and a different runtime model.

## Running checks

Common checks:

```bash
./gradlew spotlessApply
./gradlew check
```

Module-focused checks:

```bash
./gradlew :modules:booking:check
./gradlew :apps:booking-service-app:bootJar
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

Possible future milestones:

- observability with correlation IDs, metrics, and tracing
- stronger idempotency and reconciliation for unknown outcomes
- cancellation and refund process for already approved bookings
- automatic inventory hold expiration
- Temporal-based workflow prototype in a separate branch or milestone
- richer integration and contract tests

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
