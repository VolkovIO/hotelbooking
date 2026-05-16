# Observability

Current milestone: `v0.14.0 — Minimal observability and operational readiness`.

This document describes the current observability approach for the Hotel Booking project.

The goal is not to build a full production monitoring platform. The goal is to make the local distributed flow understandable and to show clear extension points for production-grade observability.

## Scope

Implemented in this milestone:

- Spring Boot Actuator health/readiness/metrics endpoints
- shared Logback configuration
- MDC context for important business identifiers
- HTTP `X-Correlation-Id` support
- gRPC metadata propagation for booking -> inventory calls
- Kafka consumer MDC context for audit and notification services
- notification delivery context restored from persisted notification tasks
- custom Micrometer business metrics
- gRPC client deadlines for inventory calls

Not implemented yet:

- centralized log aggregation
- Prometheus/Grafana deployment
- distributed tracing with OpenTelemetry
- JSON logs for production ingestion
- alerting rules

This is an intentional milestone boundary.

## Module design

Observability infrastructure is kept separate from business modules.

Dependency direction:

```text
apps/*-service-app
  -> modules/<business-module>
  -> modules/observability

modules/<business-module>
  -> application ports only
  -> no direct dependency on modules/observability
```

Examples:

```text
modules/booking
  BookingObservabilityContext
  BookingMetrics

apps/booking-service-app
  MdcBookingObservabilityContext
  MicrometerBookingMetrics
```

This keeps the domain and application layers independent from SLF4J MDC and Micrometer.

## Logging format

Development logs include a compact context block:

```text
ctx[corr=... saga=... booking=... payment=... event=... type=...]
```

Field meaning:

| Field | Meaning |
|---|---|
| `corr` | request or business correlation id |
| `saga` | booking saga id when available |
| `booking` | booking id |
| `payment` | payment id |
| `event` | outbox/Kafka event id |
| `type` | outbox/Kafka event type |

Example booking saga log:

```text
Booking saga started: sagaId=..., bookingId=... ctx[corr=... saga=... booking=... payment= event= type=]
```

Example inventory gRPC log:

```text
Received inventory gRPC PlaceHold: ... ctx[corr=... saga=... booking=... payment= event= type=]
```

Example outbox log:

```text
Published booking event to Kafka: topic=booking.events, key=..., eventId=..., eventType=BookingConfirmed ctx[corr=... booking=... event=... type=BookingConfirmed]
```

Example notification delivery log:

```text
Sending notification through logging adapter: ... ctx[corr=... booking=... event=... type=BookingConfirmed]
```

## When fields are intentionally empty

Empty fields are not always a problem.

Examples:

| Situation | Expected empty fields | Reason |
|---|---|---|
| HTTP request before saga creation | `saga`, `booking`, `payment`, `event`, `type` | those entities do not exist yet |
| batch scheduler summary | most fields | a batch log is not tied to a single business entity |
| booking events in notification-service | `saga` | Kafka envelope does not currently carry separate `sagaId` |
| inventory reservation call | `payment`, `event`, `type` | inventory call is tied to booking/saga, not to a payment event |

For event-driven downstream services, `corr` is the primary distributed correlation id.

## Correlation model

There are two related concepts:

```text
HTTP correlation id
business/event correlation id
```

HTTP requests receive or generate `X-Correlation-Id`.

The booking saga has its own saga id. Booking and payment events use a business correlation id, which for saga-created events is usually the saga id.

Manual customer cancellation after a confirmed booking is a separate command and receives its own event correlation id. This is expected because it is a different business flow from the original booking creation saga.

## HTTP correlation

Incoming HTTP requests are processed by an MDC filter in `modules/observability`.

Behavior:

- if `X-Correlation-Id` is present and safe for logs, it is reused
- otherwise a new UUID is generated
- the effective value is returned in the response header
- MDC is cleared after request processing

Header:

```http
X-Correlation-Id: <uuid-or-safe-client-value>
```

## gRPC propagation

Booking-service calls inventory-service through gRPC.

The booking gRPC client interceptor copies MDC fields into gRPC metadata:

```text
x-correlation-id
x-saga-id
x-booking-id
x-payment-id
x-event-id
x-event-type
```

The inventory gRPC server interceptor restores these values into MDC for service-side logs.

This is not distributed tracing. It is lightweight context propagation for logs.

## Kafka consumer context

Kafka events already contain important identifiers in their envelope:

```text
eventId
eventType
aggregateId
correlationId
payload
```

Consumers parse the event first and then open an MDC scope while handling it.

Implemented consumers:

- audit-service booking events consumer
- audit-service payment events consumer
- notification-service booking events consumer

For booking events:

```text
aggregateId -> bookingId
```

For payment events:

```text
aggregateId -> paymentId
payload.bookingId -> bookingId
```

## Notification delivery context

Notification creation and delivery are separated in time.

Flow:

```text
Kafka booking event
  -> notification-service consumer
  -> notification document saved in MongoDB
  -> scheduler claims notification later
  -> sender sends notification
```

To preserve observability across this gap, notification documents store source context:

```text
sourceEventId
sourceEventType
sourceAggregateId
sourceCorrelationId
```

During delivery, the scheduler restores this context before calling the sender.

## Actuator endpoints

Each application exposes Actuator endpoints:

```http
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
GET /actuator/info
GET /actuator/metrics
GET /actuator/metrics/{metric.name}
```

Recommended exposure:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      probes:
        enabled: true
        show-details: when_authorized
```

Health endpoints are suitable for local readiness checks. Metrics and info can be restricted by application security rules where security is enabled.

## Custom metrics

### Booking service

```text
hotelbooking.booking.saga.processed
hotelbooking.booking.saga.retry.scheduled
hotelbooking.booking.outbox.published
hotelbooking.booking.outbox.publication.failed
```

Typical tags:

| Metric | Tags |
|---|---|
| `hotelbooking.booking.saga.processed` | `implementation`, `outcome` |
| `hotelbooking.booking.saga.retry.scheduled` | `implementation` |
| `hotelbooking.booking.outbox.published` | `eventType` |
| `hotelbooking.booking.outbox.publication.failed` | `eventType`, `failure` |

Examples:

```text
implementation=handmade, outcome=completed
implementation=spring-statemachine, outcome=completed
eventType=BookingConfirmed
failure=retryable
failure=terminal
```

### Payment service

```text
hotelbooking.payment.authorization.processed
hotelbooking.payment.approval.processed
hotelbooking.payment.cancellation.processed
hotelbooking.payment.outbox.published
hotelbooking.payment.outbox.publication.failed
```

Typical tags:

| Metric | Tags |
|---|---|
| `hotelbooking.payment.authorization.processed` | `outcome` |
| `hotelbooking.payment.approval.processed` | `outcome` |
| `hotelbooking.payment.cancellation.processed` | `outcome` |
| `hotelbooking.payment.outbox.published` | `eventType` |
| `hotelbooking.payment.outbox.publication.failed` | `eventType`, `failure` |

Examples:

```text
outcome=authorized
outcome=declined
outcome=existing
outcome=approved
outcome=cancelled
outcome=failed
```

### Notification service

```text
hotelbooking.notification.booking_event.processed
hotelbooking.notification.delivery.processed
```

Typical tags:

| Metric | Tags |
|---|---|
| `hotelbooking.notification.booking_event.processed` | `eventType`, `outcome` |
| `hotelbooking.notification.delivery.processed` | `channel`, `outcome` |

Examples:

```text
eventType=BookingConfirmed, outcome=created
eventType=BookingPlacedOnHold, outcome=ignored_unsupported_event
channel=email, outcome=sent
```

## Checking metrics locally

List all metrics:

```http
GET /actuator/metrics
```

Check booking saga metric:

```http
GET /actuator/metrics/hotelbooking.booking.saga.processed
```

Check payment authorization metric:

```http
GET /actuator/metrics/hotelbooking.payment.authorization.processed
```

Check notification delivery metric:

```http
GET /actuator/metrics/hotelbooking.notification.delivery.processed
```

## gRPC deadlines

Booking-service applies a per-call deadline to inventory gRPC calls.

Configuration:

```yaml
inventory:
  grpc:
    client:
      deadline: ${INVENTORY_GRPC_CLIENT_DEADLINE:PT3S}
```

The deadline is applied on each individual stub call:

```text
stub.withDeadlineAfter(...).placeHold(...)
```

It is intentionally not applied once to a singleton Spring stub bean.

If inventory does not respond before the deadline, gRPC returns `DEADLINE_EXCEEDED`, and booking maps the failure into the existing saga retry/failure path.

## Current limitations

The project currently provides local observability, not a complete production observability platform.

Conscious limitations:

- logs are text-formatted, not JSON
- logs are not shipped to a central store
- metrics are exposed by Actuator but not scraped by Prometheus in this milestone
- no distributed tracing spans yet
- no alerting rules yet

Recommended future direction:

1. add Prometheus scrape config and Grafana dashboard
2. add JSON log profile for production-like deployment
3. add OpenTelemetry traces for HTTP, gRPC and Kafka boundaries
4. add alerting for saga failures and outbox terminal failures
5. add dashboards for business flow success/failure rates

