# Workflow Engine Comparison

## Context

`v0.10.0` introduced a handmade orchestrated saga for booking creation.

The current production flow is implemented by `BookingSagaProcessManager` and durable saga state stored in the `booking_sagas` table.

`v0.11.0` keeps the handmade saga as the main implementation and prepares the project for comparison with workflow/state-machine approaches.

The goal is not to replace working code immediately. The goal is to understand the trade-offs between:

- a handmade process manager;
- Spring Statemachine;
- Temporal.

## Current implementation: handmade booking saga

The current booking saga coordinates the following process:

1. create booking and booking saga;
2. place inventory hold;
3. mark booking as `ON_HOLD`;
4. authorize payment;
5. confirm inventory hold;
6. mark booking as `CONFIRMED`;
7. approve payment;
8. mark saga as `COMPLETED`.

If payment authorization is declined, the saga compensates the already completed inventory hold:

1. release inventory hold;
2. cancel booking;
3. publish `BookingCancelled` through the booking outbox;
4. mark saga as `COMPENSATED`.

Technical failures can move the saga to `WAITING_RETRY`. The retry scheduler later resumes the saga from the persisted `current_step`.

## Handmade process manager

### How it works

The handmade process manager is explicit Java orchestration code.

It owns:

- step selection;
- retry handling;
- compensation decisions;
- saga persistence;
- transition between technical saga states.

After `v0.11.0` refactoring, step execution is delegated to reusable saga action classes.

The target shape is:

- `BookingSagaProcessManager` controls orchestration and retry loop;
- `BookingSagaActionRegistry` resolves action by `BookingSagaStep`;
- action classes execute concrete business/integration work.

Example actions:

- `HoldInventorySagaAction`;
- `AuthorizePaymentSagaAction`;
- `ConfirmBookingSagaAction`;
- `ApprovePaymentSagaAction`;
- `CancelPaymentSagaAction`;
- `ReleaseInventorySagaAction`;
- `CancelBookingSagaAction`.

### Strengths

- Very explicit.
- Easy to debug step by step.
- No additional infrastructure.
- Good for learning distributed transaction boundaries.
- Easy to show in code review and interviews.
- Failure and compensation logic is visible in application code.

### Weaknesses

- Workflow logic is implemented manually.
- Retry and timeout handling must be maintained by the application.
- Long-running workflow visibility is limited.
- Harder to evolve if flows become more complex.
- No built-in workflow history UI.
- No built-in durable timers beyond what the application implements.

### Good fit

This approach is a good fit when:

- the process is relatively small;
- the team wants full control;
- external workflow infrastructure would be too heavy;
- the goal is learning and making transaction boundaries explicit.

## Spring Statemachine

### How it would work

Spring Statemachine can model the booking saga as states, events, transitions, guards, and actions.

In this project, it would not replace the domain model or integration ports. It would replace part of the handmade step-selection logic.

The current `BookingSagaAction` classes can be reused as transition actions.

Possible state machine states:

- `STARTED`;
- `HOLDING_INVENTORY`;
- `AUTHORIZING_PAYMENT`;
- `CONFIRMING_BOOKING`;
- `APPROVING_PAYMENT`;
- `COMPENSATING`;
- `COMPLETED`;
- `COMPENSATED`;
- `FAILED`.

Possible events:

- `START`;
- `INVENTORY_HELD`;
- `PAYMENT_AUTHORIZED`;
- `PAYMENT_DECLINED`;
- `BOOKING_CONFIRMED`;
- `PAYMENT_APPROVED`;
- `TECHNICAL_FAILURE`;
- `RETRY`;
- `COMPENSATION_COMPLETED`.

### Strengths

- Flow is expressed as states and transitions.
- Guards and actions are first-class concepts.
- Good for visualizing state transition logic.
- Runs inside the Spring application.
- No separate workflow server is required.
- Useful as a comparison step after handmade saga.

### Weaknesses

- Not a full durable workflow platform by itself.
- Persistence, retry, scheduling, and recovery still need careful design.
- Can add abstraction without solving distributed workflow durability completely.
- Debugging may become harder if the state machine configuration is more complex than the original code.

### Good fit

Spring Statemachine is a good fit when:

- the main complexity is state transition logic;
- the workflow remains inside one application;
- the team wants a structured state machine abstraction;
- full workflow infrastructure is not needed yet.

### Suggested role in this project

For `v0.11.0`, Spring Statemachine should be added only as a disabled prototype profile.

Recommended profile:

- `booking-saga-statemachine-prototype`

The default production-like flow should remain the handmade saga.

## Temporal

### How it would work

Temporal would model the booking process as a durable workflow.

The workflow would contain orchestration logic. Calls to inventory-service, payment-service, and booking persistence would be represented as activities.

Possible activities:

- `PlaceInventoryHoldActivity`;
- `AuthorizePaymentActivity`;
- `ConfirmInventoryHoldActivity`;
- `ConfirmBookingActivity`;
- `ApprovePaymentActivity`;
- `CancelPaymentActivity`;
- `ReleaseInventoryActivity`;
- `CancelBookingActivity`.

Temporal would require additional runtime concepts:

- Temporal service;
- worker process;
- task queue;
- workflow implementation;
- activity implementation;
- workflow client.

### Strengths

- Durable workflow execution.
- Workflow history.
- Built-in retry policies.
- Built-in timers and timeouts.
- Better support for long-running processes.
- Stronger operational visibility for workflow execution.
- Worker crash/restart recovery model.

### Weaknesses

- Requires additional infrastructure.
- Requires a different programming model.
- Activities must be designed to be idempotent.
- Adds operational and local-development complexity.
- Too heavy if the only goal is a small internal state machine.

### Good fit

Temporal is a good fit when:

- workflows are long-running;
- retries and timeouts are central to the business process;
- workflow history and visibility are important;
- multiple services participate in a durable business process;
- the team is ready to operate workflow infrastructure.

### Suggested role in this project

Temporal should not be added directly in `v0.11.0` unless the goal changes.

For now, the best approach is:

- document how the current saga maps to Temporal workflows and activities;
- keep handmade saga as the working baseline;
- add Spring Statemachine prototype first;
- consider Temporal later as a separate milestone or branch.

## Comparison table

| Criterion | Handmade process manager | Spring Statemachine | Temporal |
|---|---|---|---|
| Additional infrastructure | No | No | Yes |
| Flow visibility | Java code | State machine config | Workflow history/UI |
| Durable execution | Implemented manually | Needs explicit design | Built in |
| Retry model | Implemented manually | Needs explicit design | Built in through retry policies |
| Timers/timeouts | Implemented manually | Limited / app-managed | Built in |
| Compensation logic | Java code | Transition/actions | Workflow code/activities |
| Learning value | Very high | High | Very high but heavier |
| Operational complexity | Low | Medium | High |
| Best use in this project | Main baseline | Prototype comparison | Future milestone/comparison |

## Why the project starts with handmade saga

The handmade saga was intentionally implemented first because it makes the distributed process explicit.

This helps demonstrate understanding of:

- local transaction boundaries;
- external calls outside DB transactions;
- payment authorization versus approval;
- inventory hold versus confirmation;
- outbox-based event publication;
- compensation after partial success;
- retry after technical failures;
- difference between business failures and technical failures.

Starting with a workflow engine too early could hide these decisions behind framework abstractions.

## Recommended direction for `v0.11.0`

`v0.11.0` should be focused and not introduce too many competing production flows.

Recommended scope:

1. Extract booking saga actions from the handmade process manager.
2. Keep the handmade process manager as the default flow.
3. Add this comparison document.
4. Add Spring Statemachine prototype behind a disabled profile.
5. Do not add Temporal runtime yet.

## Recommended interview explanation

The project uses a handmade orchestrated saga as the baseline implementation.

I intentionally started with a handmade process manager because it makes transaction boundaries, compensation, retries, and idempotency concerns visible in the code.

After that, I extracted reusable saga actions so the orchestration mechanism can be changed without duplicating business logic.

This allows the same booking process to be compared with a state-machine-based approach such as Spring Statemachine, and later with a durable workflow platform such as Temporal.

The default implementation remains simple and explicit, while the project still demonstrates awareness of production workflow-engine trade-offs.

## Future work

Possible future milestones:

- add Spring Statemachine prototype behind profile;
- add diagram comparing state transitions;
- add Temporal design document;
- add Temporal prototype in a separate branch or milestone;
- add cancellation/refund process as a separate business workflow;
- add workflow observability metrics;
- add correlation IDs across booking, inventory, payment, notification, Kafka events.
