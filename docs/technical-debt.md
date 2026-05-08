# Technical Debt

## Purpose

This document tracks intentional limitations, unfinished areas and known follow-up work.

The project is educational and is being developed step by step. Some technical debt is accepted intentionally to keep each milestone focused and understandable.

## Current version

Current project version: `0.8.0`

Implemented milestones:

- `v0.5.2` security and architecture hardening.
- `v0.6.0` transactional booking outbox foundation.
- `v0.6.1` outbox polling publisher.
- `v0.6.2` inventory gRPC mTLS.
- `v0.7.0` Kafka booking event publication.
- `v0.8.0` notification service foundation.

## Cross-service architecture

### No full distributed transaction

The system intentionally does not use a distributed transaction across Booking Service, Inventory Service and Notification Service.

Current approach:

- Booking Service owns booking state in PostgreSQL.
- Inventory Service owns inventory state in MongoDB.
- Notification Service owns notification state in MongoDB.
- Kafka is used for asynchronous integration.
- Outbox pattern is used for reliable booking event publication.

Future saga/process-manager work is planned for later milestones.

### No full saga yet

Booking confirmation and cancellation workflows are not yet coordinated by an explicit saga/process manager.

Planned future work:

- payment service
- payment events
- booking process manager
- compensation logic
- timeout handling
- inventory release on payment failure
- saga state persistence

Target milestone: later than `v0.8.0`.

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

Booking events are serialized as JSON.

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
- priority settings
- fallback channels
- per-channel enabled flags

### Notification templates

Notification messages are currently static.

Examples:

- `Your booking has been confirmed.`
- `Your booking has been cancelled.`

Future improvements:

- template storage
- event-specific variables
- localization
- channel-specific formatting
- template versioning
- preview tooling

### Skipped notification channel placeholder

When a user has no notification preference, the service creates a `SKIPPED` notification.

Currently skipped notifications use:

- `channel = EMAIL`
- `destination = skipped`

This is a technical placeholder to keep the aggregate simple.

A future refactoring may allow skipped notifications without delivery channel and destination.

### Delivery retries are basic

Delivery retry behavior is implemented at a basic level.

Current behavior:

- failed delivery can be retried
- max attempts are configurable
- retry delay is configurable
- delivery claiming prevents normal duplicate sending across instances

Future improvements:

- exponential backoff
- provider-specific retry classification
- transient vs permanent error classification
- DLQ for permanently failed notifications
- operational dashboards
- alerting on high failure rate

### Delivery claiming needs operational visibility

Notification delivery uses Mongo-based claim fields:

- `lockedBy`
- `lockedUntil`

This makes scheduler operation safer with multiple instances.

Future improvements:

- metrics for claimed notifications
- metrics for expired locks
- metrics for sent and failed notifications
- admin inspection endpoint
- stuck notification diagnostics

### History API is intentionally simple

Notification history API currently supports:

- filtering by user id
- limit parameter
- newest-first sorting

Current limits:

- default limit: `10`
- maximum limit: `100`

Future improvements:

- cursor pagination
- filtering by status
- filtering by notification type
- filtering by date range

## Observability

Current logging is useful for local development, but not enough for production.

Future improvements:

- structured logs with correlation id
- service metrics
- Kafka consumer lag metrics
- outbox publication metrics
- notification delivery metrics
- tracing across HTTP, gRPC and Kafka
- dashboards
- alerts

## Testing

Current testing focuses on selected domain and service behavior.

Future improvements:

- more domain tests
- application service tests
- Mongo repository integration tests
- Kafka consumer integration tests
- Testcontainers-based tests
- contract tests for Kafka events
- security tests

## Documentation

Documentation is updated incrementally per milestone.

Important docs:

- `README.md`
- `docs/security-model.md`
- `docs/outbox.md`
- `docs/kafka.md`
- `docs/notification.md`
- `docs/technical-debt.md`

Future improvements:

- architecture diagrams
- event catalog
- API examples
- local troubleshooting guide
- production-readiness checklist
