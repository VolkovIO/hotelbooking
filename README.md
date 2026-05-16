# Hotel Booking — Senior Java Learning Project

Hotel Booking is an educational backend project for practicing production-style Java development with Spring Boot, Clean Architecture and service-to-service integration.

The goal is not to build a commercial booking platform. The goal is to demonstrate backend engineering decisions that are common in real systems and to keep those decisions explainable during interviews.

## What this project demonstrates

- Clean Architecture / Hexagonal Architecture
- Domain-Driven Design basics: aggregates, value objects, invariants
- multi-module Gradle project structure
- PostgreSQL persistence for transactional business state
- MongoDB persistence for document-oriented projections and notification tasks
- Kafka-based asynchronous integration
- transactional outbox pattern
- gRPC service-to-service communication
- HTTP integration between services
- orchestration saga with compensation and retry
- comparison of handmade orchestration and Spring Statemachine
- service-level integration testing with Testcontainers
- concurrency protection for finite inventory
- local observability: Actuator, MDC logs, correlation IDs, Micrometer metrics
- GitHub Actions CI
- Demo UI application

The project is intentionally developed in small milestones. Each milestone adds a focused architectural capability and keeps the implementation understandable.

## Current milestone

Current version:

```text
v0.15.0 — Minimal Demo UI application
```
```text
docs/v0.15.0-release-notes.md
```

## Services

### Booking service

Responsible for:

- creating bookings
- running the booking saga
- holding and confirming inventory through gRPC
- authorizing and approving payments through payment-service
- cancelling bookings
- writing booking lifecycle events to the booking outbox
- publishing booking events to Kafka
- exposing the main booking API
- exposing a Spring Statemachine saga comparison endpoint

Main HTTP port:

```text
8080
```

### Inventory service

Responsible for:

- hotel and room type inventory
- room availability
- temporary inventory holds
- hold confirmation
- hold release
- confirmed reservation cancellation
- protecting finite room availability under concurrent hold attempts

Booking-service communicates with inventory-service through gRPC.

Main HTTP port:

```text
8081
```

Main gRPC port:

```text
9090
```

### Notification service

Responsible for:

- consuming booking events from Kafka
- creating idempotent notification tasks
- storing notification source context for later delivery
- delivering notifications through a logging sender adapter

The logging sender is intentionally used for local/demo runs. It makes the flow deterministic and does not require external email, Telegram or Max credentials.

Main HTTP port:

```text
8082
```

### Payment service

Responsible for:

- payment authorization
- payment approval
- payment cancellation
- fake payment provider integration for local scenarios
- writing payment lifecycle events to the payment outbox
- publishing payment events to Kafka

The fake provider supports successful and declined authorization scenarios. No real card processing is implemented.

Main HTTP port:

```text
8083
```

### Audit service

Responsible for:

- consuming booking events from Kafka
- consuming payment events from Kafka
- projecting events into a MongoDB booking timeline read model
- exposing a booking timeline API

Timeline API:

```http
GET /api/v1/bookings/{bookingId}/timeline
```

Main HTTP port:

```text
8084
```

## Local infrastructure

Docker Compose provides local infrastructure:

- PostgreSQL
- MongoDB
- Kafka
- Kafka UI

PostgreSQL local setup uses one PostgreSQL container with separate logical databases:

```text
hotelbooking          -> booking-service
hotelbooking_payment  -> payment-service
```

MongoDB local setup uses separate logical databases or collections per service where needed.


## Local ports and startup prerequisites

Before starting the local stack, make sure these host ports are free:

| Component | Host port | Purpose |
|---|---:|---|
| booking-service | 8080 | main booking API and Swagger |
| inventory-service | 8081 | inventory HTTP API and Swagger |
| notification-service | 8082 | notification API |
| payment-service | 8083 | payment API and Swagger |
| audit-service | 8084 | booking timeline API and Swagger |
| Kafka UI | 8089 | Kafka UI from Docker Compose |
| inventory gRPC | 9090 | booking -> inventory synchronous integration |
| Kafka | 9092 | local Kafka broker |
| PostgreSQL | 5432 | local transactional database |
| MongoDB | 27017 | local document database |

Local mTLS certificates are required when the default inventory gRPC TLS settings are enabled.
Generate them once after cloning the repository:

```bash
./scripts/generate-dev-mtls-certs.sh
```

The generated files are written to `certs/dev/` and are intentionally ignored by Git.

Demo inventory data can be loaded from `docker/mongo/init/demo-data.js` or created manually through inventory admin Swagger. The scripted demo data contains fixed hotel and room type ids used by the demo requests.

More details:

```text
docs/local-development.md
docs/security.md
```

## Local run profile

For local development and demo scenarios, each service can be started with:

```text
--spring.profiles.active=dev
```

The `dev` profile group is intended to enable the normal local integration path:

- local persistence
- Kafka publishing/consumption
- dev security where applicable
- logging sender for notifications
- booking saga comparison endpoint
- shared observability configuration

Older `local` / `local-kafka` profiles may be kept for compatibility, but `dev` is the preferred profile for the current milestone.


### Local dev security

The `dev` profile uses development-friendly authentication where applicable.
For booking-service, the current user is automatically resolved as:

```text
provider: DEV
subject:  dev-user
email:    dev@example.com
name:     Development User
roles:    USER, ADMIN
```

This is only for local/demo usage. In the Google JWT profile, booking-service maps an authenticated Google account to an internal application user and enforces booking ownership through that internal `UserId`. Regular users can access only their own bookings; admin role support is modeled separately.

## Main booking saga flow

Happy path:

```text
Client
  -> booking-service: create booking saga
  -> inventory-service: place hold
  -> payment-service: authorize payment
  -> inventory-service: confirm hold
  -> booking-service: confirm booking
  -> payment-service: approve payment
  -> Kafka: BookingPlacedOnHold, BookingConfirmed
  -> Kafka: PaymentAuthorized, PaymentApproved
  -> audit-service: project timeline events
  -> notification-service: create and deliver confirmation notification
```

Final state:

| Component | State |
|---|---|
| Booking | `CONFIRMED` |
| BookingSaga | `COMPLETED` |
| Payment | `APPROVED` |
| Inventory | confirmed reservation |
| Notification | confirmation notification sent |

Declined payment path:

```text
create booking
place inventory hold
authorize payment -> DECLINED
release inventory hold
cancel booking
publish BookingCancelled
create and deliver cancellation notification
```

Final state:

| Component | State |
|---|---|
| Booking | `CANCELLED` |
| BookingSaga | `COMPENSATED` |
| Payment | `DECLINED` |
| Inventory | hold released |
| Notification | cancellation notification sent |

## Saga implementations

The project contains two saga orchestration implementations:

| Endpoint | Implementation | Purpose |
|---|---|---|
| `POST /api/v1/bookings/saga` | Handmade process manager | default production-like implementation |
| `POST /api/v1/bookings/saga-statemachine` | Spring Statemachine | comparison and learning prototype |

Both implementations reuse the same saga action classes. This keeps the comparison focused on orchestration style rather than duplicated business logic.

More details:

```text
docs/booking-saga.md
docs/workflow-engine-comparison.md
```

## Observability overview

The project has a minimal observability layer suitable for local development and portfolio demonstration.

### Logging context fields

Logs include a compact MDC context:

```text
ctx[corr=... saga=... booking=... payment=... event=... type=...]
```

Meaning:

| Field | Meaning |
|---|---|
| `corr` | request or business correlation id |
| `saga` | booking saga id when available |
| `booking` | booking id |
| `payment` | payment id |
| `event` | Kafka/outbox event id |
| `type` | Kafka/outbox event type |

Not every field is expected to be present in every log line. For example, batch-level scheduler logs may not have a specific booking or event, while individual outbox and delivery logs do.

### Actuator endpoints

Each application exposes health and metrics through Spring Boot Actuator:

```http
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
GET /actuator/metrics
```

Health endpoints are suitable for local readiness checks. Metrics endpoints expose JVM, HTTP and custom business metrics.

### Business metrics

Examples of custom metrics:

```text
hotelbooking.booking.saga.processed
hotelbooking.booking.outbox.published
hotelbooking.payment.authorization.processed
hotelbooking.payment.outbox.published
hotelbooking.notification.booking_event.processed
hotelbooking.notification.delivery.processed
```

More details:

```text
docs/observability.md
```

## Running checks

Common checks:

```bash
./gradlew spotlessApply
./gradlew check
```

Focused checks:

```bash
./gradlew :modules:booking:check
./gradlew :modules:payment:check
./gradlew :modules:notification:check
./gradlew :apps:booking-service-app:check
```

Before opening a pull request:

```bash
./gradlew clean check
```

## Demo flow

A compact demo script is documented in:

```text
docs/demo-runbook.md
```

It covers:

- starting local infrastructure
- starting services with `dev` profile
- creating a booking saga
- checking logs across services
- checking the audit timeline
- checking custom Actuator metrics
- comparing handmade and Spring Statemachine saga execution

## Known trade-offs

The project is intentionally educational and milestone-based. Some features are consciously out of scope for the current release:

- no real payment provider integration
- no real email/Telegram/Max notification sender
- no full centralized observability stack yet
- no OpenTelemetry tracing yet
- no automatic inventory hold expiration yet
- no refund workflow for customer cancellation after an approved payment yet
- Spring Statemachine is a comparison prototype, not the default orchestration engine

These trade-offs are documented in:

```text
docs/technical-debt.md
```

## Demo UI

The project includes a minimal React + Vite + TypeScript demo UI in:

```text
apps/demo-ui
```

The UI demonstrates the main local business flow:

```text
Inventory Admin -> Hotels -> Booking saga -> My bookings -> Audit timeline
```

It is a thin client for the backend services, not a replacement for API-level tests.

### Auth modes

The UI supports two modes:

| Mode | UI behavior | Booking service profile |
|---|---|---|
| `google` | signs in with Google and sends `Authorization: Bearer <google-id-token>` | `dev-jwt` |
| `demo` | does not send an authorization header; backend uses demo user | `dev` |

The committed default environment uses Google mode:

```text
apps/demo-ui/.env
```

To run locally without Google, create:

```text
apps/demo-ui/.env.local
```

with:

```env
VITE_AUTH_MODE=demo
```

Do not commit `.env.local`.

### Run UI locally

```bash
cd apps/demo-ui
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

The UI uses Vite proxy routes:

| UI route | Backend service |
|---|---|
| `/booking-api` | booking-service |
| `/inventory-api` | inventory-service |
| `/audit-api` | audit-service |

See `apps/demo-ui/README.md` and `docs/demo-ui-runbook.md` for the full demo scenario.



## Interview discussion points

This repository can be used to discuss:

- how to structure Spring Boot services with Clean Architecture
- how domain invariants are kept separate from infrastructure
- why PostgreSQL and MongoDB are used for different data shapes
- why transactional outbox is used before Kafka publication
- how Kafka consumers are made idempotent
- why booking uses a saga instead of a distributed transaction
- how compensation differs from rollback
- how gRPC is used for internal synchronous calls
- why gRPC deadlines are applied per call
- how MDC context is propagated across HTTP, gRPC and Kafka boundaries
- how Micrometer metrics expose business outcomes
- how handmade orchestration compares with Spring Statemachine
- how Testcontainers verifies persistence and concurrency behavior

