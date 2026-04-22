# Tech Debt / DDD Notes

Project: hotelbooking  
Current baseline: v0.1.0  
Updated after: module boundary cleanup and availability flow refactoring  
Started: 2026-04-20

Purpose: keep track of known design issues, deferred improvements, and architecture decisions while the domain model is still evolving.

How to use this file:
- Keep active items in **Active**
- Move intentionally delayed items to **Postponed**
- Mark finished items with `[x]` and move them to **Completed**
- When relevant, add the version where the fix was delivered

---

## Active

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

- [ ] Add architecture boundary checks
  - Area: test architecture
  - Why: protect package/module rules over time
  - Options: ArchUnit or future multi-module Gradle setup
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

- [x] Clear `holdId` after booking confirmation and cancellation
  - Fixed in: v0.1.0
  - Notes:
    - introduced held-booking-specific transitions
    - `holdId` is cleared as part of the aggregate state transition
    - removed old ambiguous confirm/cancel path from `Booking`

- [x] Revisit booking confirm/cancel flow
  - Fixed in: v0.1.0
  - Notes:
    - introduced `confirmHeldBooking()` and `cancelHeldBooking()`
    - booking state transition is now explicit and aligned with active hold lifecycle

- [x] Replace generic state failures with domain-specific exceptions
  - Fixed in: v0.1.0
  - Notes:
    - booking state transition checks now use domain exceptions instead of generic state failures
    - this keeps invalid transitions inside domain language

- [x] Prevent configured availability from overwriting held/booked state
  - Fixed in: v0.1.0
  - Notes:
    - removed the old single flow that recreated availability records
    - existing availability is now adjusted instead of silently reset

- [x] Validate capacity reduction against held/booked rooms
  - Fixed in: v0.1.0
  - Notes:
    - added domain validation in `RoomAvailability.adjustCapacity(...)`
    - total rooms cannot be set below already occupied capacity

- [x] Separate "configure availability" from "adjust capacity"
  - Fixed in: v0.1.0
  - Notes:
    - split old `SetRoomAvailabilityUseCase`
    - introduced:
      - `InitializeRoomAvailabilityUseCase`
      - `AdjustRoomCapacityUseCase`
    - controller endpoints were aligned with the new scenarios

- [x] Remove unused room availability lookup
  - Fixed in: v0.1.0
  - Notes:
    - removed unused `InventoryLookupPort.isRoomTypeAvailable()`
    - removed dead availability check path instead of keeping misleading pre-check logic

- [x] Remove `inventory.domain` leakage into `booking.application`
  - Fixed in: v0.1.0
  - Notes:
    - removed direct dependency from booking application layer to inventory domain exceptions
    - moved exception translation to the integration adapter / port boundary
    - bounded context interaction now goes through port-level contracts

- [x] Hide internal classes where possible
  - Fixed in: v0.1.0
  - Notes:
    - made internal infrastructure implementations package-private where practical
    - reduced accidental exposure of in-memory repositories and internal adapters

---

## Review points per version

### Completed in v0.1.0
- booking hold lifecycle cleanup
- clearer booking state transitions
- availability initialization vs capacity adjustment split
- capacity validation against occupied rooms
- removal of unused availability pre-check logic
- reduced cross-context leakage between booking and inventory
- reduced visibility of internal infrastructure implementations

### Planned for next milestone
- add Mongo persistence
- revisit transactional boundaries
- tighten architectural protection with automated boundary checks
