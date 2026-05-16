# Trade-offs and Production Considerations

This project is a backend portfolio case study. Some decisions are intentionally simplified to keep the system runnable locally and explainable in interviews.

## Thin UI

The React UI is intentionally small. It is used to demonstrate backend behavior, not to showcase frontend architecture.

Production considerations:

- stronger form validation;
- better error UX;
- proper user/account screens;
- role-based admin UI;
- e2e tests.

## Fake payment provider

Payment service uses a deterministic fake provider.

Reason:

- no real credentials;
- deterministic success/decline scenarios;
- easy compensation demo.

Production considerations:

- provider-specific idempotency keys;
- webhook handling;
- reconciliation;
- PCI/security requirements;
- better failure taxonomy.

## Notification logging sender

Notification delivery uses a logging adapter locally.

Reason:

- no external email/messenger credentials;
- deterministic local runs;
- focus stays on event consumption and task idempotency.

Production considerations:

- email/SMS/messenger adapters;
- retry and dead-letter strategy;
- template management;
- delivery status tracking.

## Google auth for demo

Google OAuth is used to demonstrate real JWT-based current user resolution in the UI booking flow.

Production considerations:

- dedicated identity provider configuration;
- account linking;
- roles and permissions;
- refresh/session strategy;
- token audience and issuer management per environment.

## Local Docker Compose

Docker Compose is enough for local portfolio demonstration.

Production considerations:

- Kubernetes manifests or Helm charts;
- centralized secrets management;
- readiness/liveness probes;
- resource limits;
- rolling deployments;
- observability stack.

## Spring Statemachine prototype

Spring Statemachine is included as a comparison/prototype. The main saga flow remains explicit and easier to explain.

Production considerations:

- if workflow complexity grows, evaluate workflow engines such as Temporal, Camunda or Spring Statemachine;
- define persistence, retries, visibility and operational model before adopting a workflow engine.

## Screenshots

Screenshots are intentionally not required for the first portfolio release. The backend architecture, diagrams, demo scenario and interview notes are the primary artifacts.

Screenshots can be added later if the GitHub page needs more visual polish.

