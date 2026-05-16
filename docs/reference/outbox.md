# Transactional Outbox

## Purpose

The transactional outbox is used to reliably record and process booking lifecycle events.

The first goal is local atomicity:

```text
booking state change
  + booking outbox event insert
```

Both records must be committed in the same PostgreSQL transaction.

The second goal is reliable asynchronous processing:

```text
booking_outbox NEW row
  -> claimed by publisher
  -> processed
  -> marked as PUBLISHED or scheduled for retry
```

This is the foundation for future Kafka publication.

---

## Current scope

Implemented in `v0.6.0`:

```text
- booking_outbox table
- booking lifecycle event model
- booking outbox repository
- transactional booking state + outbox persistence
```

Implemented in `v0.6.1`:

```text
- outbox polling service
- logging publisher adapter
- NEW -> PROCESSING -> PUBLISHED status flow
- retryable failure handling
- terminal FAILED status
- SKIP LOCKED based batch claiming
```

Not implemented yet:

```text
- Kafka publication
- dead-letter topic
- consumer inbox/idempotency
```

Kafka publication is planned for `v0.7.0`.

---

## Booking lifecycle events

Current event types:

```text
BookingPlacedOnHold
BookingConfirmed
BookingCancelled
```

These are application-level lifecycle events.

The project does not yet use domain events raised directly from the aggregate.
That can be introduced later if it becomes useful.

---

## Outbox row

Each outbox row contains:

```text
id
aggregate_type
aggregate_id
event_type
event_version
payload
status
attempts
next_attempt_at
occurred_at
locked_at
locked_by
published_at
last_error
created_at
updated_at
```

Current statuses:

```text
NEW
PROCESSING
PUBLISHED
FAILED
```

Status meaning:

```text
NEW
  event is waiting for processing

PROCESSING
  event was claimed by an outbox publisher instance

PUBLISHED
  event was successfully processed by the current publisher adapter

FAILED
  event reached max attempts and will not be retried automatically
```

---

## Event versioning

Each event has an `eventVersion`.

The first version is:

```text
1
```

Event versioning is required because event contracts can live longer than application code.

Future consumers should use both fields:

```text
event_type
event_version
```

to choose the correct deserialization and handling logic.

---

## Transaction boundary

The booking state and outbox insert transaction boundary is implemented in:

```text
BookingStateChangePersistenceService
```

The service saves:

```text
Booking aggregate
Booking outbox message
```

in one PostgreSQL transaction.

The most important rule is:

```text
bookingRepository.save(...)
bookingOutboxRepository.save(...)
```

must either both commit or both roll back.

---

## Why not keep the transaction open during gRPC calls?

Booking use cases still call inventory before persisting the booking state.

Current order:

```text
call inventory
  -> change booking state
  -> persist booking and outbox in one local transaction
```

The PostgreSQL transaction should not be kept open while the service waits for a remote gRPC call.

This avoids long database transactions around network operations.

---

## Polling publisher

Starting from `v0.6.1`, booking outbox messages can be processed by a scheduled polling publisher.

Current implementation:

```text
booking_outbox NEW rows
  -> claimed as PROCESSING
  -> published through logging adapter
  -> marked as PUBLISHED
```

The current publisher adapter is intentionally simple:

```text
LoggingBookingOutboxEventPublisher
```

It logs the event instead of sending it to Kafka.

Kafka publication is planned for `v0.7.0`.

The polling service already prepares the mechanics needed for Kafka:

```text
batch processing
status transitions
retry delay
max attempts
failure recording
SKIP LOCKED based claiming
```

---

## Claiming messages

Outbox rows are claimed by:

```text
claimBatchForProcessing(...)
```

The PostgreSQL implementation uses:

```text
FOR UPDATE SKIP LOCKED
```

This allows multiple service instances to safely claim different events without processing the same row at the same time.

Current claim flow:

```text
select NEW rows where next_attempt_at <= now
  -> lock selected rows
  -> update selected rows to PROCESSING
  -> return claimed messages
```

---

## Retry behavior

If publication succeeds:

```text
PROCESSING -> PUBLISHED
published_at is set
last_error is cleared
```

If publication fails and attempts are not exhausted:

```text
PROCESSING -> NEW
attempts = attempts + 1
next_attempt_at = now + retryDelay
last_error is stored
```

If publication fails and max attempts are reached:

```text
PROCESSING -> FAILED
attempts = attempts + 1
last_error is stored
```

The current implementation handles expected publisher failures through:

```text
BookingOutboxPublicationException
```

Unexpected runtime failures should not be treated as normal publication failures.

---

## Logging publisher adapter

The logging adapter is used only to validate outbox mechanics before Kafka is introduced.

Current behavior:

```text
log eventId
log eventType
log eventVersion
log aggregateType
log aggregateId
log payload
```

This adapter will be replaced or complemented by a Kafka adapter in `v0.7.0`.

---

## What this does not solve yet

The transactional outbox does not make this operation atomic:

```text
PostgreSQL booking database
  + MongoDB inventory database
  + gRPC network call
```

Known remaining risk:

```text
inventory operation succeeds
booking persistence fails
=> booking and inventory may become inconsistent
```

This is expected at this stage.

The larger cross-service consistency problem will be addressed later by the booking process manager / saga.

---

## Planned event flow

Current `v0.6.1` flow:

```text
Booking use case
  -> inventory operation
  -> booking state change
  -> booking_outbox insert

Outbox scheduler
  -> claim NEW messages
  -> logging publisher
  -> mark PUBLISHED / retry / FAILED
```

Planned `v0.7.0` flow:

```text
Booking use case
  -> booking_outbox insert

Outbox scheduler
  -> claim NEW messages
  -> Kafka producer
  -> booking.events topic
  -> mark PUBLISHED / retry / FAILED
```

---

## Future Kafka topic

Planned topic:

```text
booking.events
```

The exact topic naming convention will be documented in the Kafka release.

---

## Future event envelope

Future Kafka messages should use an envelope similar to:

```json
{
  "eventId": "...",
  "eventType": "BookingConfirmed",
  "eventVersion": 1,
  "aggregateType": "Booking",
  "aggregateId": "...",
  "occurredAt": "...",
  "correlationId": "...",
  "causationId": "...",
  "payload": {}
}
```

`correlationId` and `causationId` are not mandatory for the current logging adapter,
but they should be introduced before saga and observability work.

---

## Future work

Planned for `v0.7.0`:

```text
- Kafka producer
- booking.events topic
- event envelope publication
- dead-letter strategy
```

Planned for later releases:

```text
- inbox table for consumers
- idempotent event handling
- correlation and causation ids
- saga/process manager integration
- audit event consumption
```
