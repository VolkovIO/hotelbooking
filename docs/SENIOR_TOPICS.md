# Senior Topics Demonstrated

This project is designed to demonstrate backend engineering topics that are commonly discussed in senior Java interviews.

## Architecture

- Multi-module Gradle structure.
- Clean Architecture / Hexagonal Architecture.
- Domain model separated from frameworks.
- Application use cases and ports.
- Adapter-based persistence and web integration.
- Clear service ownership and bounded contexts.

## Domain and consistency

- Booking aggregate lifecycle.
- Inventory availability and finite resource constraints.
- Local transaction boundaries.
- Eventual consistency between services.
- Explicit compensation instead of distributed transactions.
- Idempotent event consumption patterns.

## Distributed systems

- Saga orchestration.
- Transactional outbox.
- Kafka event publication.
- Kafka consumers for projections and side effects.
- gRPC service-to-service communication.
- HTTP integration with payment service.
- Correlation IDs across synchronous and asynchronous boundaries.

## Security

- Google ID token authentication in demo flow.
- JWT validation in booking-service.
- Current user resolution from security context.
- Separate dev/demo security mode for local runs.
- Public catalog endpoints vs authenticated booking operations.

## Observability

- MDC-based structured context in logs.
- Colored local logs for development profiles.
- Correlation, saga, booking, payment and event identifiers in logs.
- Actuator health and metrics endpoints.
- Audit timeline read model built from Kafka events.

## Testing

- Service-level integration tests.
- Testcontainers-based infrastructure tests.
- Inventory contention tests for last-room scenarios.
- Saga compensation scenarios.

## Engineering trade-offs

- Thin UI to keep focus on backend architecture.
- Fake payment provider for deterministic local scenarios.
- Logging notification sender instead of external delivery credentials.
- Local Docker Compose instead of production Kubernetes setup.
- Spring Statemachine kept as comparison/prototype, not the primary implementation.

