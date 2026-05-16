# Interview Notes

Use this file as a quick preparation guide before discussing the project.

## How to describe the project

Hotel Booking is a production-style Java backend portfolio project. It models a distributed hotel booking flow with inventory reservation, payment authorization, saga orchestration, outbox-based event publishing, Kafka consumers and audit timeline projection.

The focus is not the UI. The UI is a thin demo client that makes the backend flow visible.

## Why saga?

Booking requires several operations owned by different services:

1. create booking;
2. hold inventory;
3. authorize payment;
4. confirm booking;
5. approve payment.

A distributed transaction would tightly couple services and infrastructure. The saga coordinates local transactions and uses compensation when a later step fails.

## Why outbox?

A service should not commit business state to the database and then publish Kafka event in a separate unreliable step. If the process crashes after DB commit and before Kafka send, the event is lost.

The outbox pattern stores the event in the same local transaction as business data. A publisher later sends the event to Kafka and marks it as published.

## Why gRPC between booking and inventory?

Inventory hold is a synchronous business decision required before the booking flow can continue. gRPC provides a typed service contract and efficient service-to-service communication.

## Why Kafka?

Kafka is used where the system can be eventually consistent:

- notification task creation;
- audit timeline projection;
- payment/booking event propagation.

This decouples side effects from the main booking transaction.

## How is overselling avoided?

Inventory owns room availability and hold logic. Booking-service cannot directly modify inventory state. Concurrent hold attempts are handled inside inventory-service, where finite availability is checked and updated within the service boundary.

## What happens when payment is declined?

The fake payment provider declines amounts above the configured threshold. The booking saga compensates the completed steps, releases inventory hold and marks the booking flow accordingly. The audit timeline can show the resulting events.

## How is current user resolved?

Two modes are supported:

- `dev`: fixed demo user, useful for local backend tests;
- `dev-jwt`: Google ID token is validated and current user is resolved from the JWT identity.

## How would this change in production?

Potential production improvements:

- replace fake payment provider with real provider integration;
- use external identity provider configuration and proper user management;
- deploy services with Kubernetes/Helm;
- add centralized logs and metrics dashboards;
- add stronger idempotency guarantees for all external calls;
- add contract tests for inter-service APIs;
- add alerting and dead-letter topic strategy.

## What to emphasize

- Service boundaries are explicit.
- Local transactions are preferred over distributed transactions.
- Saga compensation is modeled directly.
- Outbox prevents lost integration events.
- Kafka consumers build decoupled projections.
- Observability is part of the design, not an afterthought.

