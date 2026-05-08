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
- production-like technical debt tracking

## Current version

Current project version: `0.9.0`

Implemented milestones:

- `v0.5.2` security and architecture hardening
- `v0.6.0` transactional booking outbox foundation
- `v0.6.1` outbox polling publisher
- `v0.6.2` inventory gRPC mTLS
- `v0.7.0` Kafka booking event publication
- `v0.8.0` notification service foundation
- `v0.9.0` payment service foundation

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

Booking Service owns booking state.

Main responsibilities:

- create booking
- confirm booking
- cancel booking
- interact with Inventory Service through gRPC
- persist booking lifecycle events into a transactional outbox
- publish booking events to Kafka

Storage:

- PostgreSQL database `hotelbooking`

Important integration points:

- gRPC client to Inventory Service
- Kafka producer for `booking.events`

### Inventory Service

Inventory Service owns hotel, room and inventory state.

Main responsibilities:

- expose public hotel catalog APIs
- manage inventory
- handle booking hold/confirmation/cancellation commands through gRPC
- protect internal gRPC communication with mTLS

Storage:

- MongoDB database used by Inventory Service

Public catalog endpoints are accessible without user authentication.

Admin endpoints are protected by local development security in the current version.

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

### Payment Service

Payment Service owns payment state.

Current capabilities:

- manages payment lifecycle
- uses JPA/Hibernate persistence
- uses Liquibase-managed PostgreSQL schema
- exposes payment API
- exposes Swagger UI for local testing
- uses a fake payment provider
- persists payment lifecycle events into a transactional outbox
- publishes payment events to Kafka

Storage:

- PostgreSQL database `hotelbooking_payment`

Important integration points:

- Kafka producer for `payment.events`

Payment Service is not yet orchestrated by Booking Service. Full booking-payment workflow coordination is planned for a later saga/process-manager milestone.

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

Typical local profile:

```text
local
```

Kafka-enabled local profile:

```text
local-kafka
```

JWT-enabled local profile:

```text
local-jwt
```

JWT and Kafka-enabled local profile:

```text
local-jwt-kafka
```

### Inventory Service

Typical local profile:

```text
local
```

Inventory Service uses local development security for admin operations.

### Notification Service

Typical local profile:

```text
local
```

Expected profile group:

- `notification-mongo`
- `notification-kafka`
- `notification-senders-logging`

Run Notification Service locally:

```bash
gradlew.bat :apps:notification-service-app:bootRun --args="--spring.profiles.active=local"
```

### Payment Service

Typical local profile without Kafka publisher:

```text
local
```

Kafka-enabled local profile:

```text
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

- [Security model](docs/security-model.md)
- [Outbox](docs/outbox.md)
- [Kafka](docs/kafka.md)
- [Notification Service](docs/notification.md)
- [Payment Service](docs/payment.md)
- [Technical debt](docs/technical-debt.md)
