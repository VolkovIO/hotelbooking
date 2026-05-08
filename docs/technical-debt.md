# Technical Debt

## Purpose

This document tracks intentional limitations, unfinished areas and known follow-up work.

The project is educational and is being developed step by step. Some technical debt is accepted intentionally to keep each milestone focused and understandable.

## Current version

Current project version: `0.9.0`

Implemented milestones:

- `v0.5.2` security and architecture hardening.
- `v0.6.0` transactional booking outbox foundation.
- `v0.6.1` outbox polling publisher.
- `v0.6.2` inventory gRPC mTLS.
- `v0.7.0` Kafka booking event publication.
- `v0.8.0` notification service foundation.
- `v0.9.0` payment service foundation.

## Cross-service architecture

### No full distributed transaction

The system intentionally does not use a distributed transaction across Booking Service, Inventory Service, Payment Service and Notification Service.

Current approach:

- Booking Service owns booking state in PostgreSQL.
- Inventory Service owns inventory state in MongoDB.
- Payment Service owns payment state in PostgreSQL.
- Notification Service owns notification state in MongoDB.
- Kafka is used for asynchronous integration.
- Outbox pattern is used for reliable event publication.

Future saga/process-manager work is planned for later milestones.

### No full saga yet

Booking confirmation and payment authorization are not yet coordinated by an explicit saga/process manager.

Current limitation:

- Booking Service can create and confirm bookings.
- Payment Service can authorize, approve and cancel payments.
- These flows are still manually tested as separate service capabilities.
- Booking Service does not yet command Payment Service.
- Payment events do not yet drive Booking Service state transitions.

Planned future work:

- booking process manager
- payment authorization step in booking workflow
- inventory hold compensation on payment failure
- payment cancellation compensation on booking cancellation
- timeout handling
- saga state persistence
- correlation and causation propagation across services

Target milestone: `v0.10.0`.

## Local infrastructure

### One PostgreSQL container with multiple logical databases

For local development, Booking Service and Payment Service share one PostgreSQL container.

Current local setup:

```text
container: hotelbooking-postgres

databases:
- hotelbooking
- hotelbooking_payment
```

This is intentional for the educational project. It reduces local infrastructure overhead while preserving logical database ownership per service.

Current limitation:

- one local PostgreSQL user is used for both databases
- database-level user isolation is not modeled yet

Future production-like improvement:

- separate database users per service
- least-privilege grants
- independent migration permissions
- separate physical database instances if operationally required

## Booking Service

### Outbox relay is still basic

Booking Service publishes events from the transactional outbox.

Current limitations:

- no dedicated outbox metrics
- no dead-letter topic for permanently failed publication
- no admin endpoint for outbox inspection
- no advanced alerting
- no payload schema registry

Future improvements:

- producer metrics
- DLQ strategy
- operational dashboard
- event schema evolution policy
- replay tooling

### Correlation and causation IDs are basic

Booking outbox events currently contain:

- `correlationId`
- `causationId`

Current limitation:

- `correlationId` is initially derived from event id in simple flows.
- `causationId` is not fully propagated across all use cases.

Future improvements:

- propagate correlation id from incoming HTTP requests
- propagate correlation id through gRPC calls
- propagate correlation id through Kafka consumers and producers
- use causation id when one event causes another event

## Inventory Service

### Local development admin model

Inventory Service currently uses a development security model for local admin operations.

Current limitation:

- local admin is a mock/dev identity
- no real admin user management
- no production-grade authorization model

Future improvements:

- real admin authentication
- role management
- audit logging for inventory changes

### Public catalog access

Public catalog endpoints are intentionally accessible without authentication.

This is expected because users should be able to browse hotels and rooms before login.

Security must remain explicit:

- public catalog endpoints are allowed
- modifying endpoints require admin authorization
- deny-by-default rules should remain in place

## gRPC and mTLS

### Local certificates are development-only

mTLS certificates generated for local development are not production certificates.

Current limitations:

- local certificate generation script is for developer machines
- certificate rotation is not implemented
- production CA process is not defined

Future improvements:

- production certificate management
- certificate rotation
- certificate expiration monitoring
- stronger service identity model

## Kafka

### No schema registry yet

Booking and payment events are serialized as JSON.

Current limitations:

- no schema registry
- no compatibility checks
- no formal schema version migration process

Future improvements:

- schema registry
- compatibility tests
- explicit event contracts
- consumer-driven contract tests

### DLQ is not implemented yet

Current producer and consumer flows do not have a complete dead-letter strategy.

Future improvements:

- topic-level DLQ naming convention
- DLQ payload format
- DLQ replay process
- monitoring and alerts

## Notification Service

Notification Service was introduced in `v0.8.0`.

### Logging senders only

Notification Service currently uses logging sender adapters only.

Real delivery providers are not implemented yet:

- email provider
- Telegram bot integration
- MAX integration

This is intentional for the current milestone. The application layer already uses sender ports, so real providers can be added later without changing the core delivery flow.

### API security

Notification preference and history APIs are not protected by real authentication and authorization yet.

Current endpoints:

- `PUT /api/v1/notification-preferences/{userId}`
- `GET /api/v1/notification-preferences/{userId}`
- `GET /api/v1/notifications?userId=...`

Before production use, the service must ensure that users can only manage and read their own notification preferences and history.

Future improvements:

- authenticate user requests
- derive user id from security context
- prevent users from reading or modifying another user's preferences
- add admin-only support endpoints if needed

### User profile integration

Notification Service currently stores notification preferences directly through its own API.

In a larger production system, user contact data and notification preferences may come from a dedicated user/profile service.

Preferred future approach:

- Profile Service publishes user preference events.
- Notification Service consumes these events.
- Notification Service builds its own local read model.
- Booking Service remains decoupled from notification delivery details.

Booking Service should not call Notification Service to decide where to send notifications.

Booking Service should only publish business events.

### One preference per user

Current model supports one notification preference per user.

Current fields:

- user id
- channel
- destination
- enabled flag

Future improvements:

- multiple channels per user
- event-specific preferences
- quiet hours
- fallback channels
- preference history

### Skipped notification placeholder values

When no notification preference is found, the service creates a `SKIPPED` notification with technical placeholder values.

Current placeholder behavior:

- `channel = EMAIL`
- `destination = skipped`
- `lastError = notification preference was not found`

This is acceptable for the current milestone because it keeps the audit trail observable.

Future improvement:

- model skipped notification destination more explicitly
- avoid using a real channel enum value for a skipped/no-destination case if the domain evolves

## Payment Service

Payment Service was introduced in `v0.9.0`.

### Fake provider only

Payment Service currently uses a fake provider adapter.

Current limitation:

- no real acquiring integration
- no real card authorization
- no provider callbacks/webhooks
- no refunds
- no reconciliation
- no provider-side idempotency keys beyond local fake behavior

This is intentional for `v0.9.0`. The goal is to model the payment lifecycle and prepare for saga orchestration without depending on external payment infrastructure.

Future improvements:

- real provider adapter behind the existing provider port
- provider idempotency keys
- provider callback processing
- reconciliation job
- refund support
- payment operation audit log

### No card data model

The service does not accept or store card data.

This is intentional. The project should avoid handling sensitive payment instrument data.

Future provider integration should prefer hosted payment pages, tokenized payment methods or provider-side card storage.

### Payment API security

Payment API is not protected by production authentication and authorization yet.

Current endpoints:

- `POST /api/v1/payments/authorize`
- `POST /api/v1/payments/{paymentId}/approve`
- `POST /api/v1/payments/{paymentId}/cancel`
- `GET /api/v1/payments/{paymentId}`

Current limitation:

- caller can provide arbitrary `userId`
- service does not derive user identity from security context
- service does not verify that caller owns the booking/payment

Future improvements:

- protect public/user endpoints
- derive user id from authenticated principal
- restrict payment access by user ownership
- separate internal service API from external user API
- use mTLS or service token for internal commands from Booking Service or saga orchestrator

### Payment is not integrated with Booking Service yet

Payment Service can be tested independently, but Booking Service does not use it yet.

Current limitation:

- booking can be confirmed without payment orchestration
- payment events are published but not consumed by Booking Service
- no compensation is triggered from payment failure

Planned future work:

- booking process manager
- inventory hold + payment authorization workflow
- booking confirmation after successful payment
- inventory release after payment decline
- payment cancel after booking cancellation
- timeout handling

Target milestone: `v0.10.0`.

### Payment outbox relay is basic

Payment Service publishes payment events from a transactional outbox.

Current limitations:

- no dedicated outbox metrics
- no DLQ for permanently failed payment event publication
- no admin endpoint for inspecting failed outbox records
- no replay tooling
- retry policy is basic

Future improvements:

- producer metrics
- failed outbox dashboard
- DLQ topic
- replay endpoint or maintenance command
- structured last error details
- alerting on stuck outbox rows

### Payment correlation and causation are basic

Payment outbox events contain:

- `correlationId`
- `causationId`

Current limitation:

- `correlationId` is initially derived from event id in simple flows
- `causationId` is not yet propagated from incoming commands
- no cross-service trace context is propagated into payment events yet

Future improvements:

- accept correlation id from incoming HTTP/internal commands
- propagate booking saga correlation id
- set causation id when payment events are caused by booking commands or saga commands
- include trace ids in logs and event metadata

### One payment per booking

Current model assumes one payment per booking.

This is enforced by a unique constraint on:

```text
payment_payments.booking_id
```

Current limitation:

- no partial payments
- no multiple payment attempts per booking
- no payment method switching
- no split payment

This is acceptable for the current milestone.

Future improvements:

- explicit payment attempt model
- multiple attempts per booking
- retry after decline
- support for alternative payment methods

### Currency validation is basic

Current domain validates only that currency is a non-blank 3-letter code and normalizes it to uppercase.

Current limitation:

- no ISO currency allow-list
- unsupported 3-letter strings can pass validation

Future improvements:

- allow-list supported currencies
- explicit business rule for supported markets
- currency-specific minor unit validation

### JPA persistence has limited test coverage

Payment domain and application services are covered by unit tests.

Current limitation:

- no dedicated persistence integration tests for JPA mappings
- no Testcontainers-based PostgreSQL tests for Liquibase + Hibernate validate

Future improvements:

- repository integration tests
- Liquibase migration tests
- Hibernate mapping validation tests
- optimistic locking tests if versioning is introduced

## Future release direction

Planned next major milestone:

```text
v0.10.0 booking-payment saga / process manager
```

Expected topics:

- explicit saga state
- booking payment orchestration
- compensation logic
- payment decline handling
- inventory release on failed payment
- event correlation propagation
- timeout handling
- retry policy
