# Transactional Outbox

## Purpose

The transactional outbox is used to reliably record booking lifecycle events.

The goal is to make this operation atomic:

```text
booking state change
  + booking outbox event insert
```

Both records must be committed in the same PostgreSQL transaction.

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

Not implemented yet:

```text
- outbox polling publisher
- Kafka publication
- retries
- dead-letter topic
- consumer inbox/idempotency
```

These are planned for later releases.

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

Current initial status:

```text
NEW
```

Future publisher statuses:

```text
NEW
PROCESSING
PUBLISHED
FAILED
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

The transaction boundary is implemented in:

```text
BookingStateChangePersistenceService
```

The service saves:

```text
Booking aggregate
Booking outbox message
```

in one transaction.

This is the most important rule of the current implementation:

```text
bookingRepository.save(...)
bookingOutboxRepository.save(...)
```

must either both commit or both roll back.

---

## Why not keep the transaction open during gRPC calls?

Booking use cases still call inventory before persisting the booking state.

The intended order is:

```text
call inventory
  -> change booking state
  -> persist booking and outbox in one local transaction
```

The PostgreSQL transaction should not be kept open while the service waits for a remote gRPC call.

This avoids long database transactions around network operations.

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

Current `v0.6.0` flow:

```text
Booking use case
  -> inventory operation
  -> booking state change
  -> booking_outbox insert
```

Planned `v0.6.1` flow:

```text
Booking use case
  -> booking_outbox insert

Outbox polling publisher
  -> reads NEW events
  -> marks event as PROCESSING
  -> publishes or simulates publication
  -> marks event as PUBLISHED or FAILED
```

Planned `v0.7.0` flow:

```text
Booking use case
  -> booking_outbox insert

Outbox polling publisher
  -> Kafka producer
  -> booking.events topic
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

`correlationId` and `causationId` are not mandatory for the first outbox table implementation,
but they should be introduced before saga and observability work.

---

## Future work

Planned for `v0.6.1`:

```text
- polling publisher
- batch selection
- retry attempts
- status transitions
- failure recording
- SKIP LOCKED based locking
```

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
