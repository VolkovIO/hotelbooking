# Event Timeline Integration

`audit-service` provides a read model for inspecting what happened to a booking across service boundaries.

The goal is not to implement a legal audit log. The goal is to make the distributed booking flow observable and explainable during development, testing, and portfolio review.

## Why this exists

A booking flow is distributed across several services:

- `booking-service`
- `payment-service`
- Kafka
- transactional outbox publishers
- audit timeline projection

Without a timeline, it is hard to answer a simple question:

> What happened to this booking?

The timeline API answers this question by collecting domain events from different Kafka topics and projecting them into a single booking-oriented view.

## Current scope

The current implementation consumes:

- `booking.events`
- `payment.events`

The following events are currently projected:

- `BookingPlacedOnHold`
- `BookingConfirmed`
- `BookingCancelled`
- `PaymentAuthorized`
- `PaymentDeclined`
- `PaymentApproved`
- `PaymentCancelled`

Notification events are intentionally left out of this milestone to keep the integration slice focused.

## Timeline API

```http
GET /api/v1/bookings/{bookingId}/timeline
```

Example response shape:

```json
[
  {
    "eventType": "BookingPlacedOnHold",
    "source": "booking-service",
    "aggregateType": "Booking",
    "aggregateId": "booking-id",
    "bookingId": "booking-id",
    "correlationId": "saga-id"
  },
  {
    "eventType": "PaymentAuthorized",
    "source": "payment-service",
    "aggregateType": "Payment",
    "aggregateId": "payment-id",
    "bookingId": "booking-id",
    "correlationId": "saga-id"
  },
  {
    "eventType": "BookingConfirmed",
    "source": "booking-service",
    "aggregateType": "Booking",
    "aggregateId": "booking-id",
    "bookingId": "booking-id",
    "correlationId": "saga-id"
  },
  {
    "eventType": "PaymentApproved",
    "source": "payment-service",
    "aggregateType": "Payment",
    "aggregateId": "payment-id",
    "bookingId": "booking-id",
    "correlationId": "saga-id"
  }
]
```

## `bookingId` vs `aggregateId`

`bookingId` is the grouping key used by the timeline.

It answers:

> Which booking does this event belong to?

`aggregateId` identifies the aggregate that produced the event.

For booking events:

```text
aggregateType = Booking
aggregateId   = bookingId
bookingId     = bookingId
```

For payment events:

```text
aggregateType = Payment
aggregateId   = paymentId
bookingId     = bookingId
```

This is why both fields are present.

## `correlationId`

`correlationId` identifies one distributed business process.

For booking saga happy path, the same `correlationId` is propagated through booking-service and payment-service:

```text
BookingPlacedOnHold
PaymentAuthorized
BookingConfirmed
PaymentApproved
```

All these events share the same `correlationId`.

In the current implementation, booking saga uses:

```text
correlationId = sagaId
```

Manual booking cancellation is a separate user command and therefore receives a separate `correlationId`.

This means the following timeline is expected:

```text
BookingPlacedOnHold   correlationId = saga-id-1
PaymentAuthorized     correlationId = saga-id-1
BookingConfirmed      correlationId = saga-id-1
PaymentApproved       correlationId = saga-id-1
BookingCancelled      correlationId = cancel-flow-id-2
```

## `causationId`

`causationId` is reserved for representing the direct cause of an event.

The current implementation keeps it nullable.

Future improvements may use it to link:

- command -> event
- event -> event
- saga step -> produced domain event

## Storage and idempotency

The timeline projection is stored in MongoDB by `audit-service`.

Each timeline event is stored idempotently by `eventId`.

Duplicate Kafka delivery is expected and safe. If Kafka redelivers the same event, the projection treats it as duplicate and does not create a second timeline entry.

## Run 

Create a booking through the saga endpoint:

```http
POST /api/v1/bookings/saga
```

Then query the timeline:

```http
GET /api/v1/bookings/{bookingId}/timeline
```

## Current limitations

The timeline is intentionally simple.

It currently does not provide:

- pagination
- filtering
- search
- user-facing UI
- legal audit guarantees
- notification events
- explicit saga lifecycle events
- full `causationId` propagation
- refund/cancellation payment flow for confirmed booking cancellation

These are intentionally left for future milestones.
