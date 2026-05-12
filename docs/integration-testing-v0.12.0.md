# Integration Testing and Concurrency Guarantees

Current milestone: `v0.12.0 — Integration tests + concurrency safety + CI`.

This document describes the first service-level integration tests added to the hotel booking project and the concurrency guarantees they verify.

The goal of this milestone is not to add a new user-facing feature. The goal is to make the existing booking flow safer and more demonstrable by proving important distributed-system invariants with automated tests.

## Why this milestone matters

The project already contains a booking saga that coordinates:

- booking creation
- inventory hold placement
- payment authorization
- inventory confirmation
- booking confirmation
- payment approval
- compensation for declined payment
- booking lifecycle event publication through outbox
- notification processing through Kafka

At this stage, manual verification is no longer enough. The system needs automated checks for scenarios that are easy to break during refactoring.

The first high-value scenario is last room contention:

```text
Many clients try to book the same last available room concurrently.
Only one client may win.
The system must not over-hold or overbook inventory.
```

This scenario is especially important for a hotel booking domain because inventory is finite and concurrent requests are expected.

## Test structure

The project separates fast module tests from service-level integration tests.

Recommended convention:

```text
modules/*
  unit tests
  application/service tests
  domain invariant tests
  no full Spring Boot application context unless really needed

apps/*
  service-level integration tests
  Spring Boot application context
  real infrastructure through Testcontainers
  wiring and adapter behavior
```

This milestone follows that convention.

## Inventory-level concurrency test

Test class:

```text
apps/inventory-service-app/src/test/java/com/example/hotelbooking/inventoryservice/InventoryLastRoomContentionIntegrationTest.java
```

Purpose:

```text
Verify that inventory-service allows only one hold when many clients compete for one available room.
```

The test uses:

- real Spring Boot inventory-service context
- real MongoDB through Testcontainers
- real inventory use cases
- real MongoDB repository adapter
- concurrent Java workers through `ExecutorService`
- `CountDownLatch` to start all clients at nearly the same time

Scenario:

```text
1. Create a hotel.
2. Add one room type.
3. Configure availability for one date with exactly one room.
4. Start 20 concurrent hold attempts for the same hotel, room type, and stay period.
5. Count successful holds.
6. Read final persisted availability from MongoDB.
```

Expected result:

```text
successfulHolds = 1
heldRooms = 1
availableRooms = 0
```

This proves the inventory-level invariant:

```text
Number of successful holds must not exceed available room count.
```

### Why MongoDB is real in this test

The concurrency bug is not only a domain-object problem. It is a persistence-boundary problem.

A fragile implementation may do this:

```text
read availability
check availableRooms in Java
calculate updated heldRooms
save availability
create hold
```

Under concurrency, two threads may read the same old state:

```text
T1 reads availableRooms = 1
T2 reads availableRooms = 1
T1 decides hold can be placed
T2 decides hold can be placed
```

A pure unit test cannot reliably catch this. The test needs real persistence behavior, so it uses MongoDB Testcontainers.

## Inventory fix: atomic hold reservation

The inventory hold placement was changed from read-check-save style updates to atomic conditional updates.

The MongoDB adapter now exposes atomic operations through the application port:

```text
tryPlaceHold(hotelId, roomTypeId, date, rooms)
releaseHold(hotelId, roomTypeId, date, rooms)
```

The important operation is `tryPlaceHold(...)`.

Conceptually, it performs:

```text
increment heldRooms by rooms
only if:
  document id matches hotelId + roomTypeId + date
  totalRooms - heldRooms - bookedRooms >= rooms
```

This means the availability check and the update happen as one atomic document update.

Result under contention:

```text
T1 successfully increments heldRooms from 0 to 1
T2 tries the same update, but the condition is no longer true
T2 receives business rejection
```

For multi-day stays, the application service reserves each stay date atomically. If a later date cannot be reserved, already reserved dates are released in reverse order.

Trade-off:

```text
This is not a full distributed transaction.
It is a pragmatic per-document atomic update with application-level rollback.
For the current educational project, this is a good balance of correctness, simplicity, and interview value.
```

## Booking-level saga contention test

Test class:

```text
apps/booking-service-app/src/test/java/com/example/hotelbooking/bookingservice/BookingSagaContentionIntegrationTest.java
```

Purpose:

```text
Verify that booking-service saga handles concurrent last-room contention correctly.
```

The test uses:

- real Spring Boot booking-service context
- real PostgreSQL through Testcontainers
- real booking repositories
- real booking saga process manager
- real booking state transitions
- controlled test double for inventory reservation
- controlled test double for payment client

The real inventory-service is not started in this test.

This is intentional.

The inventory-level atomic invariant is already covered by `InventoryLastRoomContentionIntegrationTest` using real MongoDB. The booking-level test focuses on booking saga behavior when inventory returns exactly one success and many business rejections.

Scenario:

```text
1. Start 20 concurrent booking saga commands.
2. All commands target the same hotel, room type, and stay period.
3. Test inventory double allows exactly one `placeHold(...)` call to succeed.
4. All other `placeHold(...)` calls fail with `RoomHoldFailedException`.
5. Test payment client authorizes and approves successful payment requests.
6. Final booking and saga states are loaded from PostgreSQL.
```

Expected result:

```text
completedSagas = 1
failedSagas = 19
confirmedBookings = 1
rejectedBookings = 19
payment authorize calls = 1
payment approve calls = 1
payment cancel calls = 0
```

This proves the booking-level invariant:

```text
When inventory rejects competing clients, booking-service must not leave losing bookings as active NEW bookings.
```

Losing bookings are rejected because inventory hold was never placed.

## Why booking test uses test doubles

A full end-to-end scenario with real booking-service, inventory-service, payment-service, Kafka, PostgreSQL, and MongoDB would be valuable, but it would also be much heavier.

This milestone intentionally splits the problem:

```text
Inventory-level test:
  real inventory-service + real MongoDB
  verifies atomic inventory invariant

Booking-level test:
  real booking-service + real PostgreSQL
  controlled inventory/payment behavior
  verifies saga behavior and final booking states
```

This gives high confidence while keeping tests understandable and maintainable.

A future milestone may add full-stack end-to-end tests through Docker Compose or dedicated system-test infrastructure.

## CI workflow

GitHub Actions workflow:

```text
.github/workflows/ci.yml
```

The workflow runs on:

```text
pull_request -> master
push -> master
```

Main command:

```bash
./gradlew check --no-daemon --stacktrace
```

The workflow sets up:

- Ubuntu runner
- Java 21
- Gradle cache
- Gradle wrapper execution permission

On failure, it uploads reports as artifacts:

```text
**/build/reports/tests/**
**/build/reports/checkstyle/**
**/build/reports/pmd/**
**/build/reports/spotbugs/**
```

This makes the concurrency tests part of the pull request quality gate.

## Local verification commands

Inventory contention test:

```bash
./gradlew :apps:inventory-service-app:integrationTest --tests "*InventoryLastRoomContentionIntegrationTest"
```

Booking saga contention test:

```bash
./gradlew :apps:booking-service-app:integrationTest --tests "*BookingSagaContentionIntegrationTest"
```

Focused checks:

```bash
./gradlew :modules:inventory:check
./gradlew :apps:inventory-service-app:check
./gradlew :apps:booking-service-app:check
```

Full verification:

```bash
./gradlew check
```

On Windows PowerShell or cmd, use:

```bash
.\gradlew check
```

## Current guarantees

After this milestone, the project has automated coverage for:

```text
Inventory-level last room contention:
  exactly one concurrent hold succeeds for one available room

Booking-level saga contention:
  exactly one booking is confirmed
  losing bookings are rejected
  losing sagas are failed
  payment is called only for the winning booking

CI:
  checks run automatically on pull requests and master pushes
```

## Known limitations

This milestone does not yet cover:

- full end-to-end multi-service contention through real HTTP/gRPC services
- Kafka assertions for booking events in the contention scenario
- notification assertions for rejected/failed contention losers
- load testing with k6
- OpenTelemetry tracing
- Prometheus/Grafana metrics
- ELK/Loki centralized log search
- branch protection requiring CI before merge

These are candidates for later milestones.

## Interview discussion points

This milestone is useful for discussing:

- why concurrency bugs often live at the persistence boundary
- why read-check-save is unsafe under contention
- how atomic conditional updates protect finite inventory
- how application-level rollback works for multi-day reservations
- where Testcontainers are useful
- difference between integration tests and full end-to-end tests
- why booking-level tests can use controlled test doubles
- why CI should run regression tests automatically
