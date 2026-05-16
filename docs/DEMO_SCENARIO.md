# Demo Scenario

This scenario is designed for a short portfolio demonstration.

## Goal

Show how the system handles a booking flow across multiple services:

```text
Google sign-in -> inventory availability -> booking saga -> payment -> Kafka events -> audit timeline
```

## Prerequisites

- Docker infrastructure is running.
- Backend services are started.
- Demo UI is running on `http://localhost:5173`.
- Google OAuth client allows `http://localhost:5173`.

Recommended profiles:

| Service | Profile |
|---|---|
| `booking-service` | `dev-jwt` |
| all other backend services | `dev` |

## Happy path

1. Open Demo UI.
2. Sign in with Google.
3. Open `Inventory Admin`.
4. Create hotel.
5. Add room type.
6. Initialize availability for future dates.
7. Open `Hotels`.
8. Select hotel and room type.
9. Check availability.
10. Start booking saga with payment amount `1500`.
11. Open `My bookings`.
12. Verify booking status.
13. Open `Timeline` and inspect events.

## Compensation path

Repeat the booking flow with:

```text
paymentAmount > 50000
```

Expected behavior:

- fake payment provider declines authorization;
- saga compensates already completed steps;
- inventory hold is released;
- booking is cancelled/rejected by the saga flow;
- audit timeline shows the business events.

## Cancellation path

1. Create a successful booking.
2. Open `My bookings`.
3. Click `Cancel` for an active booking.
4. Open timeline again.
5. Verify cancellation event.

## What to highlight during interview

- Why booking is coordinated by saga instead of distributed transactions.
- Where local transaction boundaries are located.
- How outbox avoids losing events after DB commit.
- How inventory is protected against overselling.
- How correlation IDs make the flow traceable.
- Why audit timeline is a read model built from events.

