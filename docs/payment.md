# Payment Service

## Purpose

Payment Service is responsible for managing the payment lifecycle for hotel bookings.

In the current version, the service uses a fake payment provider. No real card processing, acquiring integration, refunds or external payment callbacks are implemented yet.

The goal of this milestone is to introduce a payment bounded context, persist payment state, expose a local API for testing the payment lifecycle and publish payment lifecycle events through a transactional outbox.

## Current scope

Implemented in `v0.9.0`:

- `payment-service` Spring Boot application.
- Payment business module.
- Payment domain model.
- PostgreSQL persistence through JPA/Hibernate.
- Liquibase-managed database schema.
- Fake payment provider adapter.
- Payment use cases.
- Payment REST API.
- Swagger UI for local API testing.
- Payment transactional outbox.
- Kafka publisher for payment lifecycle events.

The service is intentionally not connected to Booking Service yet. Full booking-payment orchestration is planned for a later saga/process-manager milestone.

## Module structure

Payment Service follows the same modular style as the rest of the project.

Application module:

```text
apps/payment-service-app
```

Business module:

```text
modules/payment
```

Main module areas:

- `domain`
- `application`
- `application ports`
- `inbound web adapters`
- `outbound persistence adapters`
- `outbound provider adapters`
- `outbound Kafka messaging adapters`

## Local database ownership

Payment Service owns its own logical PostgreSQL database.

For local development, Booking Service and Payment Service share one PostgreSQL container, but they use different logical databases:

```text
PostgreSQL container: hotelbooking-postgres

Databases:
- hotelbooking
- hotelbooking_payment
```

This mirrors the local MongoDB setup where several services may share one local MongoDB instance while still using separate logical databases.

Main payment database:

```text
hotelbooking_payment
```

Main tables:

- `payment_payments`
- `payment_outbox`
- `databasechangelog`
- `databasechangeloglock`

The service does not share tables with Booking Service.

## Persistence approach

Payment Service uses JPA/Hibernate for persistence mapping and Liquibase for schema management.

Responsibilities are intentionally separated:

```text
Liquibase  -> creates and migrates database schema
Hibernate  -> maps entities to already existing tables
```

Hibernate schema generation is not used for creating tables.

Expected setting:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

This allows Hibernate to validate that entity mappings match the Liquibase-managed schema.

## Payment lifecycle

Current payment statuses:

- `NEW`
- `AUTHORIZED`
- `DECLINED`
- `APPROVED`
- `CANCELLED`

Current state transitions:

```text
NEW -> AUTHORIZED
NEW -> DECLINED
AUTHORIZED -> APPROVED
AUTHORIZED -> CANCELLED
```

Terminal states:

- `DECLINED`
- `APPROVED`
- `CANCELLED`

## Domain invariants

Payment domain protects the following invariants:

- payment amount must be positive
- payment currency must be a 3-letter currency code
- only `NEW` payment can be authorized
- only `NEW` payment can be declined
- only `AUTHORIZED` payment can be approved
- only `AUTHORIZED` payment can be cancelled
- `DECLINED`, `APPROVED` and `CANCELLED` are terminal states
- `AUTHORIZED` payment must have provider payment id
- `APPROVED` payment must have provider payment id and authorization timestamp
- `CANCELLED` payment must have provider payment id and authorization timestamp
- `DECLINED` payment must have failure reason and decline timestamp

These rules are enforced by the `Payment` aggregate and covered by domain unit tests.

## Fake payment provider

The current provider is a fake adapter.

Provider enum:

```text
FAKE
```

The fake provider can authorize or decline a payment depending on local configuration.

Example configuration:

```yaml
app:
  payment:
    provider:
      fake:
        enabled: true
        always-decline: false
```

The fake provider generates provider payment ids with the following prefix:

```text
fake-payment-
```

This adapter is useful for local testing and for future saga development without depending on a real payment provider.

## Payment API

Payment Service exposes a local REST API.

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

Get payment by id:

```http
GET /api/v1/payments/{paymentId}
```

Health endpoint:

```http
GET /api/v1/payments/health
```

## Swagger UI

Swagger UI is enabled for local API testing.

Local URL:

```text
http://localhost:8083/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8083/v3/api-docs
```

## Example authorize request

```json
{
  "bookingId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "amount": 12500.00,
  "currency": "RUB"
}
```

Example response:

```json
{
  "id": "b530f6aa-0696-44c7-8361-f59bb36d8830",
  "bookingId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "amount": 12500.00,
  "currency": "RUB",
  "provider": "FAKE",
  "status": "AUTHORIZED",
  "providerPaymentId": "fake-payment-4808b058-0be9-4f7c-a1f9-491cab83d6cb",
  "failureReason": null
}
```

## Idempotency

Authorization is idempotent by booking id.

If the same booking id is authorized more than once, the service returns the existing payment instead of creating a second payment.

The database enforces this with a unique constraint on:

```text
payment_payments.booking_id
```

Current assumption:

```text
one booking -> one payment
```

## Transactional outbox

Payment Service uses a transactional outbox to publish payment events reliably.

When a payment state changes, the service persists:

```text
payment state change
+
payment outbox message
```

in one PostgreSQL transaction.

This protects against the situation where payment state is committed but the corresponding Kafka event is lost before publication.

Outbox table:

```text
payment_outbox
```

Outbox statuses:

- `NEW`
- `PROCESSING`
- `PUBLISHED`
- `FAILED`

Polling publisher flow:

```text
payment_outbox NEW
        |
        v
claim batch for processing
        |
        v
publish to Kafka
        |
        v
mark PUBLISHED or retry/FAILED
```

The polling repository uses `FOR UPDATE SKIP LOCKED` to allow several service instances to claim outbox rows safely.

## Kafka publication

Payment Service publishes payment lifecycle events to Kafka.

Topic:

```text
payment.events
```

Current event types:

- `PaymentAuthorized`
- `PaymentDeclined`
- `PaymentApproved`
- `PaymentCancelled`

Event envelope contains:

- event id
- event type
- event version
- aggregate type
- aggregate id
- occurred at
- correlation id
- causation id
- payload

Example payload fields:

- `paymentId`
- `bookingId`
- `userId`
- `amount`
- `currency`
- `provider`
- `status`
- `providerPaymentId`
- `failureReason`

## Local profiles

Typical local profile without Kafka publisher:

```text
local
```

Kafka-enabled profile:

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

Run without Kafka publishing:

```bash
gradlew.bat :apps:payment-service-app:bootRun --args="--spring.profiles.active=local"
```

Run with Kafka publishing:

```bash
gradlew.bat :apps:payment-service-app:bootRun --args="--spring.profiles.active=local-kafka"
```

## Local verification

Start infrastructure:

```bash
docker compose up -d
```

Create local payment database if it does not exist yet:

```bash
docker exec -it hotelbooking-postgres psql -U hotelbooking -d hotelbooking
```

Inside `psql`:

```sql
create database hotelbooking_payment;
```

Connect to payment database:

```sql
\c hotelbooking_payment
```

Check tables:

```sql
\dt
```

Expected tables after Payment Service startup:

```text
databasechangelog
databasechangeloglock
payment_payments
payment_outbox
```

Run Payment Service with Kafka publisher:

```bash
gradlew.bat :apps:payment-service-app:bootRun --args="--spring.profiles.active=local-kafka"
```

Open Swagger UI:

```text
http://localhost:8083/swagger-ui.html
```

Authorize a payment and check database state:

```sql
select id, booking_id, status, provider_payment_id
from payment_payments
order by created_at desc;
```

Check outbox state:

```sql
select event_id, event_type, processing_status, retry_count, published_at
from payment_outbox
order by created_at desc;
```

Expected result with `local-kafka` profile:

```text
processing_status = PUBLISHED
published_at is not null
```

Consume payment events from Kafka:

```bash
docker exec -it hotelbooking-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic payment.events \
  --from-beginning \
  --max-messages 5
```

## Current limitations

Current limitations are intentional for `v0.9.0`:

- Payment Service is not yet integrated into the Booking Service workflow.
- No saga/process manager exists yet.
- Fake provider does not call a real payment system.
- No real card data is accepted or stored.
- No payment refunds are implemented.
- No external provider callbacks/webhooks are implemented.
- API is not protected by production authentication/authorization yet.
- Kafka events are JSON without schema registry.
- DLQ and replay tooling are not implemented yet.

These limitations are tracked in `technical-debt.md`.
