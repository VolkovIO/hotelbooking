# Technical Debt

## Purpose

This document tracks intentional limitations, unfinished areas and known follow-up work.

The project is educational and is being developed step by step. Some technical debt is accepted intentionally to keep each milestone focused and understandable.

## Current version

Current project version: `0.10.0`

Implemented milestones:

- `v0.5.2` security and architecture hardening.
- `v0.6.0` transactional booking outbox foundation.
- `v0.6.1` outbox polling publisher.
- `v0.6.2` inventory gRPC mTLS.
- `v0.7.0` Kafka booking event publication.
- `v0.8.0` notification service foundation.
- `v0.9.0` payment service foundation.
- `v0.10.0` booking saga orchestration.

## Cross-service architecture

### No distributed ACID transaction

The system intentionally does not use a distributed transaction across Booking Service, Inventory Service, Payment Service and Notification Service.

Current approach:

- Booking Service owns booking state in PostgreSQL.
- Inventory Service owns inventory state in MongoDB.
- Payment Service owns payment state in PostgreSQL.
- Notification Service owns notification state in MongoDB.
- Kafka is used for asynchronous integration.
- Outbox pattern is used for reliable event publication.
- Booking Service coordinates booking creation through an explicit saga process manager.

This is intentional. The project demonstrates local transactions, compensation, retries and event-driven propagation instead of XA/distributed transactions.

### Handmade saga process manager

`v0.10.0` introduced a handmade booking saga process manager.

Current capabilities:

- stores saga state in `booking_sagas`
- creates booking and saga state durably before external calls
- places inventory hold
- authorizes payment
- confirms inventory hold
- confirms booking
- approves payment
- compensates payment decline by releasing inventory hold and cancelling booking
- publishes saga-driven booking changes through booking outbox
- supports basic retry metadata and retry scheduler

Current limitations:

- this is not a workflow engine
- no visual workflow runtime
- no built-in long-running timer support beyond simple scheduler
- no advanced workflow versioning
- no operator UI for saga inspection or manual recovery

Future improvement options:

- keep handmade process manager and add operational tooling
- compare with Spring Statemachine for state-machine clarity
- compare with Temporal for durable workflow orchestration
- compare with Camunda for BPMN/process visibility

### Correlation and causation are still basic

Booking and payment outbox events contain correlation and causation identifiers.

Current limitation:

- correlation id is not consistently propagated from incoming HTTP requests
- correlation id is not consistently propagated through gRPC calls
- correlation id is not consistently propagated through payment HTTP calls
- causation id is not fully connected across saga commands and emitted events

Future improvements:

- accept correlation id from incoming HTTP headers
- store saga correlation id
- propagate correlation id to Inventory Service and Payment Service
- include correlation id in all outbox events caused by the saga
- include trace ids in logs and event metadata

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

### Saga retry handling is basic

The saga has retry metadata and a retry scheduler.

Current fields:

- `status`
- `current_step`
- `retry_count`
- `next_attempt_at`
- `last_failure_reason`

Current limitations:

- retry policy is simple and fixed-delay based
- no exponential backoff
- no jitter
- no per-step retry policy
- no dead-letter state for sagas requiring operator action
- no admin endpoint to inspect or resume stuck sagas
- no operational dashboard

Future improvements:

- per-step retry configuration
- exponential backoff with jitter
- max total saga lifetime
- manual retry endpoint for support/admin use
- stuck saga alerting
- structured failure classification

### Payment approval unknown outcome is not reconciled

A difficult production case remains unresolved.

Example:

```text
Booking Service calls Payment Service approve(paymentId).
Payment Service approves the payment.
Booking Service loses the HTTP response due to timeout or network failure.
```

Current limitation:

- Booking Service may not know whether payment approval succeeded.
- Retrying or compensating blindly can be unsafe without stronger idempotency and reconciliation.

Future improvements:

- idempotent command keys for payment approve/cancel operations
- payment operation status lookup
- reconciliation job for unknown payment outcomes
- explicit `UNKNOWN` or `PENDING_CONFIRMATION` handling for payment operations
- stronger provider-side idempotency support

### Cancellation after approved payment does not refund payment

`v0.10.0` covers compensation during booking creation.

It does not implement the separate business process of cancelling an already confirmed and approved booking.

Current limitation:

- if booking is successfully confirmed and payment is approved, later user cancellation does not refund payment
- no refund lifecycle exists in Payment Service
- no cancellation/refund saga exists yet

Future improvements:

- add refund domain model to Payment Service
- add refund events
- add cancellation/refund process manager
- define cancellation policy and fees
- integrate notification for refund status

### Saga-driven booking changes must use outbox-aware persistence

Saga-driven booking status changes must not use `BookingRepository.save(...)` directly.

Correct boundary:

```text
BookingSagaProcessManager
-> Booking aggregate state change
-> BookingStateChangePersistenceService
-> bookings + booking_outbox
-> BookingOutboxPollingService
-> Kafka booking.events
```

Current limitation:

- this rule is architectural and should be protected by tests and review discipline
- no automated architecture test enforces it yet

Future improvements:

- ArchUnit test to prevent direct booking state persistence from saga flows
- helper methods around common saga booking state changes
- stronger process manager tests verifying outbox persistence calls

### Outbox relay is still basic

Booking Service publishes events from the transactional outbox.

Current limitations:

- no dedicated outbox metrics
- no dead-letter topic for permanently failed publication
- no admin endpoint for outbox inspection
- no advanced alerting
- no payload schema registry
- Kafka client startup logs are noisy in local development

Future improvements:

- producer metrics
- DLQ strategy
- operational dashboard
- event schema evolution policy
- replay tooling
- tune Kafka client logging for local developer experience

### Automatic inventory hold expiration is not implemented

Inventory holds created during booking saga are temporary from a business perspective.

Current implementation:

- Booking saga explicitly releases hold on payment decline or compensation.
- There is no automatic hold expiration job yet.

Current limitation:

- if a process crashes or gets stuck before compensation, a hold may remain until manual cleanup or future recovery logic handles it
- no `expiresAt` field is enforced for holds
- no scheduled expired-hold release exists

Future improvements:

- add `expiresAt` to inventory holds
- add scheduled expired hold release
- make hold release idempotent
- publish `InventoryHoldExpired` or equivalent event if needed
- integrate expired hold handling with saga recovery

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

### gRPC retry/error classification is still simple

Inventory communication is performed through gRPC.

Current limitation:

- not every gRPC status is classified into retryable and non-retryable categories
- no per-method timeout/retry policy is documented at the adapter boundary
- no circuit breaker is implemented

Future improvements:

- classify gRPC statuses explicitly
- add deadlines/timeouts for all gRPC calls
- consider Resilience4j circuit breaker and retry policies
- expose metrics for inventory client failures

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

Current fake provider supports local saga testing with:

- `always-decline`
- `decline-amount-greater-than`

This is intentional. The goal is to model the payment lifecycle and prepare for saga orchestration without depending on external payment infrastructure.

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

### Payment is integrated with Booking Service only through synchronous saga commands

`v0.10.0` integrates Payment Service into Booking Service saga through direct HTTP commands.

Current limitation:

- Booking Service commands Payment Service synchronously
- Booking Service does not consume `payment.events` yet
- payment events are currently useful for observability and future integrations, not for driving booking state transitions

Future improvements:

- decide whether payment events should drive any asynchronous booking read model or reconciliation flow
- propagate saga correlation id into payment events
- add consumer-driven contract tests between Booking and Payment services

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

Possible next milestones:

```text
v0.10.x saga hardening and documentation cleanup
v0.11.0 cancellation/refund process or observability hardening
```

Candidate topics:

- refund/cancellation saga after approved payment
- inventory hold expiration
- stronger saga retry and reconciliation
- distributed tracing and correlation propagation
- outbox metrics and DLQ strategy
- contract tests between Booking, Inventory and Payment services
- comparison with workflow engines such as Temporal or Camunda
