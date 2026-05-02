# Technical Debt v0.4.0

This document records the technical state after extracting the project into Gradle modules and separately runnable service applications.

Older technical debt documents are intentionally kept in the repository to show the project's architectural evolution over time.

## Current version

`v0.4.0`

The project is now a Gradle multi-project build with separate runtime applications:

```text
apps/
  booking-service-app
  inventory-service-app

modules/
  booking
  inventory
  inventory-grpc-api
```

The project is still one repository and one codebase, but it no longer has a single Spring Boot application entry point.

Runtime is now split:

```text
booking-service-app
  -> PostgreSQL
  -> gRPC client
  -> inventory-service-app

inventory-service-app
  -> MongoDB
  -> gRPC server
```

The project remains intentionally not production-ready.

---

## Completed improvements in v0.4.0

### Gradle multi-project structure

The project was split into Gradle subprojects.

Current structure:

```text
hotelbooking/
  settings.gradle
  build.gradle

  apps/
    booking-service-app/
    inventory-service-app/

  modules/
    booking/
    inventory/
    inventory-grpc-api/
```

The root project is now a build aggregator and shared build configuration holder.

The root project no longer owns application source code.

---

### Booking module extraction

Booking code was moved into:

```text
modules/booking
```

This module contains:

- booking domain model
- booking application services
- booking use case ports
- booking HTTP API adapters
- booking PostgreSQL persistence adapter
- booking gRPC client adapter for inventory

Booking should not depend on inventory domain or inventory application classes.

Allowed dependency:

```text
modules/booking -> modules/inventory-grpc-api
```

Forbidden dependency:

```text
modules/booking -> modules/inventory
```

---

### Inventory module extraction

Inventory code was moved into:

```text
modules/inventory
```

This module contains:

- inventory domain model
- inventory application services
- inventory use case ports
- inventory HTTP API adapters
- inventory MongoDB persistence adapters
- inventory gRPC server adapters

Inventory owns hotels, room types, room availability and room holds.

---

### Inventory gRPC API module

The inventory gRPC contract was moved into:

```text
modules/inventory-grpc-api
```

The protobuf contract is located at:

```text
modules/inventory-grpc-api/src/main/proto/inventory/v1/inventory_service.proto
```

Both booking and inventory depend on this module.

Booking uses it to generate gRPC client stubs.

Inventory uses it to implement the gRPC server.

---

### Inventory service application

A separate inventory Spring Boot application was added:

```text
apps/inventory-service-app
```

It starts:

- inventory HTTP API on port `8081`
- inventory gRPC API on port `9090`
- MongoDB persistence

This application depends on:

```text
modules/inventory
modules/inventory-grpc-api
```

---

### Booking service application

A separate booking Spring Boot application was added:

```text
apps/booking-service-app
```

It starts:

- booking HTTP API on port `8080`
- PostgreSQL persistence
- gRPC client connection to inventory on `localhost:9090`

This application depends on:

```text
modules/booking
modules/inventory-grpc-api
```

It must not depend on:

```text
modules/inventory
```

---

### Single application entry point removed

The old root Spring Boot application entry point was removed.

The project is no longer started with:

```bash
./gradlew bootRun
```

Instead, applications are started separately:

```bash
./gradlew :apps:inventory-service-app:bootRun
./gradlew :apps:booking-service-app:bootRun
```

---

## Current runtime model

### Inventory service

```text
inventory-service-app
  -> modules/inventory
  -> modules/inventory-grpc-api
  -> MongoDB
```

Ports:

```text
HTTP: 8081
gRPC: 9090
```

### Booking service

```text
booking-service-app
  -> modules/booking
  -> modules/inventory-grpc-api
  -> PostgreSQL
  -> inventory-service-app via gRPC
```

Ports:

```text
HTTP: 8080
```

### Request flow

```text
Client / Swagger
  -> booking-service-app HTTP API
  -> Booking application use case
  -> InventoryLookupPort / InventoryReservationPort
  -> booking gRPC client adapter
  -> inventory-service-app gRPC API
  -> inventory gRPC server adapter
  -> Inventory application use case
  -> MongoDB
```

---

## Known technical debt

### Service-level integration tests need to be restored

During module extraction, old integration tests tied to the previous single-runtime/direct-integration setup were removed.

This was done to avoid preserving tests that verified obsolete architecture.

New integration tests should be added for the current architecture:

```text
booking-service-app
  -> gRPC
  -> inventory-service-app
```

Recommended future test types:

- booking module unit tests
- inventory module unit tests
- repository contract tests
- service-level integration tests
- gRPC adapter tests
- end-to-end booking lifecycle tests using both service applications

Possible future location:

```text
apps/booking-service-app/src/integrationTest
apps/inventory-service-app/src/integrationTest
```

or a dedicated:

```text
integration-tests/
```

module.

---

### Distributed consistency

The system now has separate runtime applications.

This makes consistency problems more explicit.

Current booking creation flow is still synchronous:

```text
booking-service
  -> inventory gRPC PlaceHold
  -> booking PostgreSQL save
```

Failure scenarios remain:

- inventory hold succeeds but booking save fails
- booking receives timeout after inventory applied the operation
- retry repeats a non-idempotent operation
- booking and inventory become temporarily inconsistent

Future improvements should include:

- idempotency keys
- command identifiers
- retry-safe inventory operations
- transactional outbox
- saga/process manager
- compensating actions

---

### Outbox is not implemented yet

The project does not yet use the outbox pattern.

This means domain events are not reliably persisted and published.

Future outbox work should clarify:

- where events are stored
- how events are published
- how publication failures are retried
- how duplicate event processing is handled
- which service owns which event stream

---

### Saga/process manager is not implemented yet

The current booking process is still synchronous.

A future saga/process manager can make the booking flow explicit:

```text
BookingRequested
  -> ReserveInventory
  -> InventoryReserved
  -> ConfirmBooking
  -> BookingConfirmed
```

It should also handle compensation:

```text
InventoryReserved
  -> BookingPersistenceFailed
  -> ReleaseInventory
```

---

### Idempotency

Current command operations are not fully idempotent.

Examples:

- confirming an already confirmed booking may be rejected
- cancelling an already cancelled booking may be rejected
- retrying a gRPC call may repeat an operation
- client timeout does not mean the operation failed

Future improvements:

- idempotency keys for external HTTP commands
- command IDs for gRPC operations
- deduplication tables
- retry-safe application services
- explicit operation status tracking

---

### gRPC error model is still simple

The current gRPC error model maps inventory-side exceptions to gRPC statuses and then maps gRPC failures back to booking-side application exceptions.

This keeps Java exception classes from leaking across service boundaries.

Future improvements:

- stable application error codes
- structured gRPC error details
- better mapping between gRPC status and HTTP status
- separate handling of validation errors, business conflicts and infrastructure failures
- mapping inventory unavailability to HTTP `503 Service Unavailable`

---

### No service discovery

The booking service currently connects to inventory through static host/port configuration:

```text
localhost:9090
```

This is enough for local development.

Future improvements may include:

- Docker Compose service DNS
- environment-specific configuration
- Kubernetes service discovery
- health checks
- readiness checks

---

### No gRPC deadlines/retries yet

The current gRPC client does not yet define a clear deadline/retry policy.

Future improvements:

- set explicit gRPC deadlines
- define retryable and non-retryable statuses
- avoid retrying non-idempotent operations
- log timeout and unavailable states clearly

---

### Observability is minimal

The project currently has limited application-level observability.

Future improvements:

- structured logs
- correlation IDs
- request IDs
- gRPC metadata propagation
- Spring Boot Actuator health checks
- Micrometer metrics
- OpenTelemetry tracing
- dashboards
- clear logs for booking lifecycle transitions

---

### Security is not implemented yet

The project does not yet have authentication or authorization.

Planned security work:

- JWT support
- Google OAuth2 login
- authenticated user identity
- associating bookings with users
- protecting admin endpoints
- separating user API from admin API

---

### API ownership and public contracts

The project now has two HTTP APIs:

```text
booking-service-app HTTP API
inventory-service-app HTTP API
```

Future work should clarify:

- which APIs are public
- which APIs are admin/internal
- which APIs should remain stable
- whether inventory HTTP endpoints should be exposed externally
- whether booking should be the main public entry point

---

### Configuration duplication

After splitting applications, some configuration is duplicated between app modules.

This is acceptable for the current learning stage.

Future improvements may include:

- shared Gradle convention plugins
- environment-specific config
- Docker Compose profiles
- `.env` support
- separate local/dev/prod config strategy

---

### Build logic is still in root build.gradle

The root `build.gradle` currently contains shared plugin and quality configuration for all subprojects.

This is acceptable for the current stage.

Future improvements may include extracting build conventions into:

```text
buildSrc
```

or:

```text
gradle convention plugins
```

This would make subproject build files smaller and more consistent.

---

### Java version

Java 21 LTS is recommended.

The project was aligned to Java 21 during module extraction.

Running on newer non-LTS JDKs may produce warnings from gRPC/Netty about deprecated `sun.misc.Unsafe` memory access APIs.

---

## Planned next steps

### v0.5.0 — Security

Planned scope:

1. Add JWT support.
2. Add Google OAuth2 login.
3. Associate bookings with authenticated users.
4. Protect admin endpoints.
5. Keep security concerns outside the domain model where possible.

### v0.6.0 — Outbox

Planned scope:

1. Add transactional outbox for booking events.
2. Publish booking lifecycle events reliably.
3. Define event contracts.
4. Prepare for asynchronous communication.

### v0.7.0 — Saga/process manager

Planned scope:

1. Introduce explicit booking process state.
2. Handle distributed booking flow.
3. Add compensation and retry behavior.
4. Make failure recovery visible and testable.

### v0.8.0 — Observability and resilience

Planned scope:

1. Add structured logging.
2. Add correlation IDs.
3. Add metrics.
4. Add tracing.
5. Add gRPC deadlines and retry strategy.
6. Add health and readiness checks.

### v1.0.0 — Portfolio-ready release

Planned scope:

1. Clean architecture documentation.
2. ADRs.
3. Diagrams.
4. Complete local startup guide.
5. Service-level integration tests.
6. Final README polish.
