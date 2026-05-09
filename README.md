# Hotel Booking

Educational hotel booking project for practicing backend architecture and Senior Java engineering topics.

The project is intentionally developed step by step through small milestones.

Main topics:

- Clean Architecture
- DDD-inspired domain modeling
- modular Gradle project structure
- Spring Boot applications
- PostgreSQL
- MongoDB
- JPA/Hibernate
- Liquibase
- transactional outbox
- Kafka
- gRPC
- mTLS
- service-to-service communication
- event-driven architecture
- notification delivery flow
- payment lifecycle modeling
- orchestrated saga / process manager
- retry and compensation flows
- production-like technical debt tracking

## Current version

Current project version: `0.10.0`

Implemented milestones:

- `v0.5.2` security and architecture hardening
- `v0.6.0` transactional booking outbox foundation
- `v0.6.1` outbox polling publisher
- `v0.6.2` inventory gRPC mTLS
- `v0.7.0` Kafka booking event publication
- `v0.8.0` notification service foundation
- `v0.9.0` payment service foundation
- `v0.10.0` booking saga orchestration

## Project structure

The project uses a multi-module Gradle structure.

Applications:

```text
apps/booking-service-app
apps/inventory-service-app
apps/notification-service-app
apps/payment-service-app
```

Business modules:

```text
modules/booking
modules/inventory
modules/inventory-grpc-api
modules/notification
modules/payment
```

## Services

### Booking Service

Booking Service owns booking state and orchestrates the booking saga.

Main responsibilities:

- create booking
- confirm booking
- cancel booking
- start booking saga orchestration
- coordinate Inventory Service through gRPC
- coordinate Payment Service through HTTP client
- persist booking lifecycle events into a transactional outbox
- publish booking events to Kafka
- schedule retry for retryable saga failures

Storage:

- PostgreSQL database `hotelbooking`

Important integration points:

- gRPC client to Inventory Service
- HTTP client to Payment Service
- Kafka producer for `booking.events`

### Inventory Service

Inventory Service owns hotel, room and inventory state.

Main responsibilities:

- expose public hotel catalog APIs
- manage inventory
- place temporary inventory holds
- confirm holds into reservations
- release temporary holds
- cancel confirmed reservations
- protect internal gRPC communication with mTLS

Storage:

- MongoDB database used by Inventory Service

Public catalog endpoints are accessible without user authentication.

Admin endpoints are protected by local development security in the current version.

### Payment Service

Payment Service owns payment state.

Current capabilities:

- manages payment lifecycle
- uses JPA/Hibernate persistence
- uses Liquibase-managed PostgreSQL schema
- exposes payment API
- exposes Swagger UI for local testing
- uses a fake payment provider
- supports fake provider decline scenarios for saga testing
- persists payment lifecycle events into a transactional outbox
- publishes payment events to Kafka

Storage:

- PostgreSQL database `hotelbooking_payment`

Important integration points:

- Kafka producer for `payment.events`
- HTTP API used by Booking Service saga process manager

Payment lifecycle used by the saga:

```text
NEW -> AUTHORIZED -> APPROVED
NEW -> DECLINED
AUTHORIZED -> CANCELLED
```

### Notification Service

Notification Service consumes booking lifecycle events from Kafka and creates user notifications.

Current capabilities:

- consumes `booking.events`
- handles `BookingConfirmed`
- handles `BookingCancelled`
- ignores unsupported booking events such as `BookingPlacedOnHold`
- stores notifications in MongoDB
- stores user notification preferences in MongoDB
- supports EMAIL, TELEGRAM and MAX channels through logging adapters
- uses idempotent event handling
- uses Mongo-based delivery claiming for multi-instance safety
- exposes notification preference API
- exposes notification history API

Storage:

- MongoDB database `hotelbooking_notification`

Notification Service does not call real external providers yet. EMAIL, TELEGRAM and MAX senders currently write to logs only.

## Booking saga orchestration

Starting from `v0.10.0`, Booking Service contains a simple explicit process manager for booking orchestration.

Endpoint:

```http
POST /api/v1/bookings/saga
```

The saga coordinates:

```text
Booking Service -> Inventory Service -> Payment Service -> Booking Outbox -> Kafka -> Notification Service
```

Happy path:

```text
1. Create Booking
2. Create BookingSaga
3. Place inventory hold
4. Mark Booking as ON_HOLD
5. Authorize payment
6. Confirm inventory hold
7. Mark Booking as CONFIRMED
8. Approve payment
9. Mark BookingSaga as COMPLETED
10. Publish BookingConfirmed to booking.events
11. Notification Service creates confirmation notification
```

Payment declined path:

```text
1. Create Booking
2. Create BookingSaga
3. Place inventory hold
4. Mark Booking as ON_HOLD
5. Authorize payment
6. Payment Service returns DECLINED
7. Release inventory hold
8. Mark Booking as CANCELLED
9. Mark BookingSaga as COMPENSATED
10. Publish BookingCancelled to booking.events
11. Notification Service creates cancellation notification
```

Retry path:

```text
1. Retryable technical failure happens during saga step execution
2. BookingSaga is moved to WAITING_RETRY
3. retryCount is incremented
4. nextAttemptAt is set
5. BookingSagaRetryScheduler later resumes the saga
```

The saga is intentionally handmade and explicit. It is not a workflow engine.

## Why payment has two steps

Payment is split into two stages:

| Step | Meaning |
|---|---|
| `authorize` | Checks and reserves the ability to pay |
| `approve` | Finalizes payment after inventory and booking are confirmed |

This avoids final payment approval before the room is actually confirmed.

If a later step fails after authorization, the saga can cancel the authorization instead of issuing a refund.

## Why inventory has two steps

Inventory is split into two stages:

| Step | Meaning |
|---|---|
| `placeHold` | Temporarily holds the room while payment is being authorized |
| `confirmHold` | Converts the temporary hold into a confirmed reservation |

If payment is declined, the temporary hold is released.

If inventory was already confirmed and a later step fails, the saga can cancel the confirmed reservation.

## Infrastructure

Local infrastructure is started through Docker Compose.

Typical infrastructure services:

- PostgreSQL
- MongoDB
- Kafka

Start infrastructure:

```bash
docker compose up -d
```

## Local database layout

For local development, some services share infrastructure containers while still owning separate logical databases.

PostgreSQL:

```text
container: hotelbooking-postgres

logical databases:
- hotelbooking
- hotelbooking_payment
```

MongoDB:

```text
one local MongoDB instance
separate logical databases for inventory and notification contexts
```

This setup keeps local development simpler while preserving service-level database ownership at the logical database level.

## Local profiles

### Booking Service

Typical local profiles:

```text
local
local-kafka
local-jwt
local-jwt-kafka
```

Expected profile groups include booking PostgreSQL support, local security where needed, Kafka publisher support, gRPC inventory client configuration and payment client configuration.

Run Booking Service locally with Kafka support:

```bash
gradlew.bat :apps:booking-service-app:bootRun --args="--spring.profiles.active=local-jwt-kafka"
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

### Inventory Service

Typical local profiles:

```text
local
security-dev
```

Inventory Service exposes public catalog APIs and internal gRPC APIs.

### Notification Service

Typical local profile:

```text
local-kafka
```

Notification Service consumes `booking.events` and writes notification delivery attempts through logging adapters.

### Payment Service

Typical local profiles:

```text
local
local-kafka
```

Expected profile groups:

```text
local:
- payment-postgres
- payment-dev

local-kafka:
- payment-postgres
- payment-dev
- payment-outbox-publisher
- payment-outbox-kafka
```

Run Payment Service locally without Kafka publisher:

```bash
gradlew.bat :apps:payment-service-app:bootRun --args="--spring.profiles.active=local"
```

Run Payment Service locally with Kafka publisher:

```bash
gradlew.bat :apps:payment-service-app:bootRun --args="--spring.profiles.active=local-kafka"
```

Swagger UI:

```text
http://localhost:8083/swagger-ui.html
```

Fake provider decline configuration example:

```yaml
app:
  payment:
    provider:
      fake:
        enabled: true
        always-decline: false
        decline-amount-greater-than: 50000.00
```

## Kafka

Booking Service publishes booking lifecycle events to Kafka.

Current booking topic:

```text
booking.events
```

Notification Service consumes this topic.

Current notification consumer group:

```text
notification-service
```

Payment Service publishes payment lifecycle events to Kafka.

Current payment topic:

```text
payment.events
```

Kafka event publication is based on transactional outbox tables in the producing services.

## Booking saga local verification

Start infrastructure:

```bash
docker compose up -d
```

Start services:

```bash
gradlew.bat :apps:inventory-service-app:bootRun --args="--spring.profiles.active=local,security-dev"
gradlew.bat :apps:payment-service-app:bootRun --args="--spring.profiles.active=local-kafka"
gradlew.bat :apps:notification-service-app:bootRun --args="--spring.profiles.active=local-kafka"
gradlew.bat :apps:booking-service-app:bootRun --args="--spring.profiles.active=local-jwt-kafka"
```

Start saga request:

```http
POST /api/v1/bookings/saga
```

Example happy path body:

```json
{
  "hotelId": "10000000-0000-0000-0000-000000000001",
  "roomTypeId": "20000000-0000-0000-0000-000000000001",
  "checkIn": "2030-06-27",
  "checkOut": "2030-06-28",
  "guestCount": 1,
  "paymentAmount": 3500,
  "paymentCurrency": "RUB"
}
```

Expected happy path result:

```text
booking.status = CONFIRMED
booking_sagas.status = COMPLETED
payment_payments.status = APPROVED
booking.events contains BookingConfirmed
notification-service sends Booking confirmed notification through logging adapter
```

Example payment declined body:

```json
{
  "hotelId": "10000000-0000-0000-0000-000000000001",
  "roomTypeId": "20000000-0000-0000-0000-000000000001",
  "checkIn": "2030-06-28",
  "checkOut": "2030-06-29",
  "guestCount": 1,
  "paymentAmount": 1500000,
  "paymentCurrency": "RUB"
}
```

Expected declined result:

```text
booking.status = CANCELLED
booking_sagas.status = COMPENSATED
payment_payments.status = DECLINED
inventory hold is released
booking.events contains BookingCancelled
notification-service sends Booking cancelled notification through logging adapter
```

Check booking outbox:

```sql
select id,
       event_type,
       aggregate_id,
       status,
       attempts,
       created_at,
       published_at,
       last_error
from booking_outbox
order by created_at desc
limit 20;
```

Check saga state:

```sql
select id,
       booking_id,
       status,
       current_step,
       payment_id,
       retry_count,
       next_attempt_at,
       last_failure_reason,
       created_at,
       updated_at
from booking_sagas
order by created_at desc
limit 20;
```

## Payment local verification

Start infrastructure:

```bash
docker compose up -d
```

Create payment database if it does not exist yet:

```bash
docker exec -it hotelbooking-postgres psql -U hotelbooking -d hotelbooking
```

Inside `psql`:

```sql
create database hotelbooking_payment;
```

Run Payment Service with Kafka publisher:

```bash
gradlew.bat :apps:payment-service-app:bootRun --args="--spring.profiles.active=local-kafka"
```

Open Swagger UI:

```text
http://localhost:8083/swagger-ui.html
```

Authorize payment:

```http
POST /api/v1/payments/authorize
```

Approve payment:

```http
POST /api/v1/payments/{paymentId}/approve
```

Cancel payment:

```http
POST /api/v1/payments/{paymentId}/cancel
```

Get payment:

```http
GET /api/v1/payments/{paymentId}
```

Check payment table:

```sql
select id, booking_id, status, provider_payment_id
from payment_payments
order by created_at desc;
```

Check payment outbox:

```sql
select event_id, event_type, processing_status, retry_count, published_at
from payment_outbox
order by created_at desc;
```

Expected with `local-kafka` profile:

```text
processing_status = PUBLISHED
published_at is not null
```

## Documentation

Project documentation:

- `docs/security-model.md`
- `docs/outbox.md`
- `docs/kafka.md`
- `docs/notification.md`
- `docs/payment.md`
- `docs/booking-saga.md`
- `docs/technical-debt.md`
