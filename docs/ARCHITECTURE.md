# Architecture Overview

Hotel Booking is a distributed backend portfolio project built around a realistic booking flow.

The architecture focuses on explicit service boundaries, reliable asynchronous integration, controlled consistency and observable distributed execution.

## Bounded contexts / services

| Service | Main responsibility | Storage |
|---|---|---|
| `booking-service` | Booking lifecycle, current user bookings, saga orchestration. | PostgreSQL |
| `inventory-service` | Hotel catalog, room types, availability, holds and reservations. | MongoDB |
| `payment-service` | Fake payment authorization/approval/cancellation and payment events. | PostgreSQL |
| `notification-service` | Notification task creation and local logging sender. | MongoDB |
| `audit-service` | Booking/payment event projection into timeline read model. | MongoDB |

## Communication style

| Integration | Style | Reason |
|---|---|---|
| UI -> services | HTTP/JSON | Simple demo and Swagger/curl compatibility. |
| booking -> inventory | gRPC | Synchronous inventory hold operation with explicit service contract. |
| booking -> payment | HTTP | Simple payment provider boundary for the portfolio scope. |
| booking/payment -> Kafka | Outbox + async publishing | Reliable event publication after local transaction commit. |
| Kafka -> audit/notification | Async consumers | Eventual consistency and decoupled projections. |

## Consistency model

The system avoids distributed transactions. Each service owns its local transaction boundary.

Booking consistency is coordinated by saga orchestration:

1. create booking;
2. hold inventory;
3. authorize payment;
4. confirm booking;
5. approve payment;
6. compensate already completed steps if a later step fails.

Kafka consumers build eventually consistent projections such as audit timeline and notification tasks.

## Security model

The demo supports two local modes:

| Mode | UI | booking-service |
|---|---|---|
| Google auth | Sends Google ID token as bearer token. | `dev-jwt` validates JWT and resolves current user. |
| Demo auth | Sends no Authorization header. | `dev` uses fixed demo user. |

Inventory catalog endpoints are public for browsing. Booking operations require a current user.

## Observability

The project includes:

- correlation IDs propagated through request/event flow;
- MDC log fields for correlation, saga, booking, payment and event IDs;
- colored local logs for development profiles;
- Actuator health/metrics endpoints;
- audit timeline projection for business-level event tracing.

## Local infrastructure

Docker Compose provides:

- PostgreSQL;
- MongoDB;
- Kafka;
- Kafka UI.

PostgreSQL local setup uses one container with separate logical databases:

```text
hotelbooking          -> booking-service
hotelbooking_payment  -> payment-service
```

