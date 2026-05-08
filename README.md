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
- transactional outbox
- Kafka
- gRPC
- mTLS
- service-to-service communication
- event-driven architecture
- notification delivery flow
- production-like technical debt tracking

## Current version

Current project version: `0.8.0`

Implemented milestones:

- `v0.5.2` security and architecture hardening
- `v0.6.0` transactional booking outbox foundation
- `v0.6.1` outbox polling publisher
- `v0.6.2` inventory gRPC mTLS
- `v0.7.0` Kafka booking event publication
- `v0.8.0` notification service foundation

## Project structure

The project uses a multi-module Gradle structure.

Applications:

```text
apps/booking-service-app
apps/inventory-service-app
apps/notification-service-app
```

Business modules:

```text
modules/booking
modules/inventory
modules/inventory-grpc-api
modules/notification
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

- PostgreSQL

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

- MongoDB

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

## Kafka

Booking Service publishes booking lifecycle events to Kafka.

Current topic:

```text
booking.events
```

Notification Service consumes this topic.

Current consumer group:

```text
notification-service
```

Kafka event publication is based on the Booking Service transactional outbox.

## Notification local verification

Start infrastructure:

```bash
docker compose up -d
```

Run Inventory Service.

Run Booking Service with Kafka profile.

Run Notification Service:

```bash
gradlew.bat :apps:notification-service-app:bootRun --args="--spring.profiles.active=local"
```

Create or update notification preference:

```bash
curl -X PUT "http://localhost:8082/api/v1/notification-preferences/2e1ecd64-e449-49a0-8744-eb5473c8e76b" ^
  -H "Content-Type: application/json" ^
  -d "{\"channel\":\"EMAIL\",\"destination\":\"user@example.com\",\"enabled\":true}"
```

Create a booking and confirm it.

Expected behavior:

- `BookingPlacedOnHold` event is ignored by Notification Service.
- `BookingConfirmed` event creates a notification.
- Delivery scheduler claims the notification.
- Logging EMAIL sender writes a log message.
- Notification status becomes `SENT`.

Get notification history:

```bash
curl "http://localhost:8082/api/v1/notifications?userId=2e1ecd64-e449-49a0-8744-eb5473c8e76b&limit=10"
```

## Build and checks

Run full check:

```bash
gradlew.bat check
```

For faster checks during development, prefer module-level commands.

Examples:

```bash
gradlew.bat :modules:notification:check
gradlew.bat :modules:notification:pmdMain
gradlew.bat :modules:notification:pmdTest
gradlew.bat :apps:notification-service-app:bootJar
```

Avoid `clean check` for every small change. `clean` deletes Gradle build outputs and forces all modules to be rebuilt.

## Documentation

Project documentation:

- [Security model](docs/security-model.md)
- [Outbox](docs/outbox.md)
- [Kafka](docs/kafka.md)
- [Notification Service](docs/notification.md)
- [Technical debt](docs/technical-debt.md)

## Current limitations

The project is not production-ready.

Known limitations include:

- no real external notification providers
- no production user/profile service
- no full saga/process manager yet
- no payment service yet
- no complete DLQ strategy
- no schema registry
- no production-grade admin model
- limited observability
- limited integration tests
- notification APIs are not protected by real authentication and authorization yet

These limitations are tracked intentionally and will be addressed in later milestones.
