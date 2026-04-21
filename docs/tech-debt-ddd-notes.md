# Tech Debt / DDD Notes

Project: hotelbooking  
Current baseline: v0.1.0  
Started: 2026-04-20

Purpose: keep track of known design issues, deferred improvements, and architecture decisions while the domain model is still evolving.

How to use this file:
- Keep active items in **Active**
- Move intentionally delayed items to **Postponed**
- Mark finished items with `[x]` and move them to **Completed**
- When relevant, add the version where the fix was delivered

---

## Active

### Domain model consistency

- [ ] Clear `holdId` after booking confirmation and cancellation
  - Area: booking domain
  - Why: avoid stale hold reference in aggregate state
  - Target version: TBD
  - Notes: current behavior is acceptable for learning phase, but should be fixed before production-like persistence

- [ ] Revisit booking confirm/cancel flow
  - Area: booking + inventory interaction
  - Why: state transitions should stay explicit and consistent
  - Target version: TBD
  - Notes: domain transition should better reflect final aggregate state

- [ ] Replace generic state failures with domain-specific exceptions
  - Area: application / domain
  - Why: avoid leaking technical exceptions such as `IllegalStateException` into API behavior
  - Target version: TBD

### Availability model

- [ ] Prevent configured availability from overwriting held/booked state
  - Area: inventory domain
  - Why: current availability setup may erase already tracked reservation state
  - Target version: TBD
  - Priority: high

- [ ] Validate capacity reduction against held/booked rooms
  - Area: inventory domain
  - Why: total capacity should not become lower than already reserved capacity
  - Target version: TBD

- [ ] Separate "configure availability" from "adjust capacity"
  - Area: inventory application / domain
  - Why: these are different business operations and should not share one ambiguous flow
  - Target version: TBD

### Module boundaries

- [ ] Remove `inventory.domain` leakage into `booking.application`
  - Area: module boundaries
  - Why: bounded contexts should communicate through ports/contracts, not foreign domain exceptions
  - Target version: TBD

- [ ] Hide internal classes where possible
  - Area: package design
  - Why: reduce accidental coupling inside the modular monolith
  - Target version: TBD

- [ ] Add architecture boundary checks
  - Area: test architecture
  - Why: protect package/module rules over time
  - Options: ArchUnit or future multi-module Gradle setup
  - Target version: TBD

### Persistence preparation

- [ ] Introduce Mongo persistence adapter for `booking`
  - Area: infrastructure.persistence.mongo
  - Why: start persistence layer without polluting domain model
  - Planned types:
    - `BookingDocument`
    - `MongoBookingRepositoryAdapter`
    - mapper between document and domain
  - Target version: TBD

- [ ] Define Mongo persistence strategy for `inventory`
  - Area: infrastructure.persistence.mongo
  - Why: inventory model includes availability, holds, and booked state that need careful document design
  - Target version: TBD

---

## Postponed

- [ ] Rework in-memory repositories to behave closer to real persistence semantics
  - Status: postponed until persistent storage is introduced
  - Why: current in-memory adapters are good enough for domain learning phase

- [ ] Add stronger modular monolith enforcement through Gradle modules
  - Status: postponed until domain flows are more stable
  - Why: package-level separation is enough for the current learning stage

- [ ] Revisit API/package naming polish and infrastructure cleanup
  - Status: postponed
  - Examples:
    - align project version in build files and tags
    - review profile defaults
    - clean up documentation drift

### Transactional risks to revisit later

- [ ] Booking creation may leave orphan hold if inventory succeeds and booking persistence fails
  - Status: postponed until persistent storage / cross-module consistency work
  - Why: current in-memory phase hides this risk

- [ ] Confirm/cancel flow may become inconsistent without a clear transaction strategy
  - Status: postponed until persistence and integration design
  - Why: especially important if modules later become microservices

- [ ] Define long-term consistency approach for possible microservice split
  - Status: postponed
  - Options to study later:
    - local transactions inside one service
    - outbox pattern
    - saga / process manager
    - idempotent command handling

---

## Completed

<!-- Example:
- [x] Added Mongo persistence adapter for booking
  - Fixed in: v0.2.0
  - Commit: abc1234
  - Notes: introduced BookingDocument and mapper
-->

---

## Review points per version

### Planned for v0.1.x
- stabilize domain flows
- keep technical debt visible
- avoid premature infrastructure overengineering

### Planned for next milestone
- add Mongo persistence
- revisit transactional boundaries
- tighten module boundaries after core model is stable
