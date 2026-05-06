# Kafka Integration

## Purpose

Kafka is used as the asynchronous event transport between services.

Starting from `v0.7.0`, booking lifecycle events are published from the booking transactional outbox to Kafka.

Current flow:

- booking use case changes booking state
- booking state and outbox event are stored in PostgreSQL
- outbox polling publisher claims NEW events
- Kafka publisher adapter sends events to Kafka
- outbox row is marked as PUBLISHED

Kafka is not used directly from booking use cases.

The booking application layer still depends only on the `BookingOutboxEventPublisher` port.

---

## Current scope

Implemented in `v0.7.0`:

- Kafka broker in Docker Compose
- `booking.events` topic configuration
- Kafka publisher adapter for booking outbox events
- booking event envelope
- Kafka key based on booking aggregate id
- producer-side error handling through outbox retry and FAILED status

Not implemented yet:

- Kafka consumers
- notification service
- payment service
- audit service
- consumer inbox pattern
- consumer DLQ processing

These are planned for later releases.

---

## Local Kafka

Kafka is started through Docker Compose.

The local broker is intended for development and portfolio demonstration.

Default local bootstrap server:

- `localhost:9092`

The booking service Kafka publisher uses:

- `KAFKA_BOOTSTRAP_SERVERS`
- default value: `localhost:9092`

---

## Profiles

The project keeps two local booking modes.

Simple local mode:

- `local`
- uses logging outbox publisher
- does not require Kafka

Kafka local mode:

- `local-kafka`
- uses Kafka outbox publisher
- requires Kafka to be running

JWT variants may also exist for manual Google JWT testing:

- `local-jwt`
- `local-jwt-kafka`

---

## Topic naming

Current topic:

- `booking.events`

Naming convention:

- `<bounded-context>.events`

Examples planned for future services:

- `booking.events`
- `payment.events`
- `notification.events`
- `audit.events`

This keeps topics aligned with bounded contexts.

---

## Booking event envelope

Kafka messages use an event envelope.

Current envelope fields:

- `eventId`
- `eventType`
- `eventVersion`
- `aggregateType`
- `aggregateId`
- `occurredAt`
- `correlationId`
- `causationId`
- `payload`

Example:

    {
      "eventId": "2fbc7282-7e2e-46c8-b853-d1f54a6e85ac",
      "eventType": "BookingConfirmed",
      "eventVersion": 1,
      "aggregateType": "Booking",
      "aggregateId": "9122f668-0266-48c3-ac6a-7bb472e0f57c",
      "occurredAt": "2026-05-06T14:07:12.123Z",
      "correlationId": "2fbc7282-7e2e-46c8-b853-d1f54a6e85ac",
      "causationId": null,
      "payload": {
        "bookingId": "9122f668-0266-48c3-ac6a-7bb472e0f57c",
        "status": "CONFIRMED"
      }
    }

---

## Event versioning

Each event has an `eventVersion`.

Current version:

- `1`

Consumers should use both fields:

- `eventType`
- `eventVersion`

This allows event contracts to evolve without breaking all consumers at once.

---

## Correlation and causation ids

Starting from `v0.7.0`, outbox messages include:

- `correlationId`
- `causationId`

Current behavior:

- `correlationId` is created for the current operation
- `causationId` is currently null for direct HTTP-command initiated events

Future saga behavior:

- one `correlationId` should connect the whole booking process
- `causationId` should point to the command or event that caused the current event

These ids will be important for:

- saga orchestration
- audit records
- distributed tracing
- troubleshooting

---

## Kafka key

Kafka message key:

- `aggregateId`
- for booking events, this means booking id

Reason:

- all events for the same booking go to the same Kafka partition
- ordering is preserved per booking aggregate

This is important for future saga and consumer processing.

---

## Producer error strategy

Producer-side errors are handled by the outbox publisher.

If Kafka publication succeeds:

- outbox status becomes `PUBLISHED`
- `published_at` is set
- `last_error` is cleared

If Kafka publication fails and attempts are not exhausted:

- outbox status becomes `NEW`
- `attempts` is incremented
- `next_attempt_at` is moved to the future
- `last_error` is stored

If max attempts are reached:

- outbox status becomes `FAILED`
- `last_error` is stored

This means Kafka producer failures do not lose events immediately.
Events remain visible in the booking database.

---

## DLQ strategy

There are two different failure areas.

### Producer-side failures

Producer-side failures happen before the event reaches Kafka.

Current strategy:

- handled by outbox retry
- final state is `FAILED`

No Kafka DLQ is needed for producer-side failures at this stage.

### Consumer-side failures

Consumer-side failures happen after the event is already in Kafka.

Future strategy:

- retry consumer processing
- record processed events in inbox table
- send poison messages to DLQ topic after retries

Future DLQ topic naming examples:

- `booking.events.dlq`
- `payment.events.dlq`
- `notification.events.dlq`

Consumer-side DLQ processing will be introduced when real consumers are added.

---

## Manual verification

Start infrastructure:

- `docker compose up -d`

Start inventory service:

- `gradlew.bat :apps:inventory-service-app:bootRun --args="--spring.profiles.active=local"`

Start booking service with Kafka publisher:

- `gradlew.bat :apps:booking-service-app:bootRun --args="--spring.profiles.active=local-kafka"`

Create, confirm or cancel a booking.

Check outbox:

    select id, event_type, status, attempts, published_at, last_error
    from booking_outbox
    order by created_at desc;

Expected result:

- `status = PUBLISHED`
- `published_at is not null`
- `last_error is null`

Read Kafka topic:

- `docker exec -it hotelbooking-kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic booking.events --from-beginning`

Expected result:

- JSON event envelope appears in `booking.events`

---

## Future work

Planned for `v0.8.0`:

- notification service foundation
- first idempotent Kafka consumer
- delivery status
- email / telegram / max sender ports

Planned for later releases:

- payment service events
- audit event consumer
- booking process manager / saga
- consumer inbox pattern
- consumer DLQ processing
- OpenTelemetry correlation with event metadata
