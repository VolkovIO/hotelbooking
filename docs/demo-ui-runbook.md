# Demo UI Runbook — v0.15.0

This runbook describes the local end-to-end demo for the `hotelbooking` platform with the React demo UI.

## Goal

Show a complete booking flow across services:

```text
Inventory Admin
  -> create hotel
  -> add room type
  -> initialize availability

Hotels
  -> check availability
  -> start booking saga

My bookings
  -> see status
  -> open audit timeline
  -> cancel booking
```

The demo intentionally keeps the UI thin. The main value is in the backend architecture: DDD modules, service boundaries, saga orchestration, outbox, Kafka integration, gRPC inventory integration, security modes, and observability.

## Profiles

### Booking service profiles

Recommended profile groups:

```yaml
spring:
  profiles:
    group:
      dev:
        - booking-postgres
        - inventory-grpc-client
        - security-dev
        - outbox-publisher
        - outbox-kafka
        - booking-saga-springstatemachine-prototype

      dev-jwt:
        - booking-postgres
        - inventory-grpc-client
        - security-jwt
        - outbox-publisher
        - outbox-kafka
        - booking-saga-springstatemachine-prototype

      local:
        - booking-postgres
        - inventory-grpc-client
        - security-dev
        - outbox-publisher
        - outbox-logging

      local-kafka:
        - booking-postgres
        - inventory-grpc-client
        - security-dev
        - outbox-publisher
        - outbox-kafka

      local-jwt:
        - booking-postgres
        - inventory-grpc-client
        - security-jwt
        - outbox-publisher
        - outbox-logging

      local-jwt-kafka:
        - booking-postgres
        - inventory-grpc-client
        - security-jwt
        - outbox-publisher
        - outbox-kafka
```

Use:

| Purpose | UI mode | booking-service profile |
|---|---|---|
| Google-authenticated demo | `google` | `dev-jwt` |
| Simple local demo without Google | `demo` | `dev` |

## Environment

The committed default UI environment should be:

```env
VITE_AUTH_MODE=google
VITE_GOOGLE_CLIENT_ID=<google-web-client-id>.apps.googleusercontent.com

VITE_BOOKING_SERVICE_BASE_URL=/booking-api
VITE_INVENTORY_SERVICE_BASE_URL=/inventory-api
VITE_AUDIT_SERVICE_BASE_URL=/audit-api
```

For temporary local demo mode, create `apps/demo-ui/.env.local`:

```env
VITE_AUTH_MODE=demo
```

Do not commit `.env.local`.

## Start checklist

### 1. Start infrastructure

Start the local infrastructure required by the services: PostgreSQL, MongoDB, Kafka and other dependencies configured by the project.

Use the project Docker Compose setup normally used for local development.

### 2. Start backend services

For Google mode:

```text
booking-service:   dev-jwt
inventory-service: local/dev profile used by the project
payment-service:   local/dev profile used by the project
audit-service:     local/dev profile used by the project
```

For demo mode:

```text
booking-service:   dev
```

The important part is that the UI auth mode and booking-service security profile must match.

### 3. Start the UI

```bash
cd apps/demo-ui
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

## Google auth setup

Google mode requires a browser OAuth client.

The OAuth client must contain the local JavaScript origin:

```text
http://localhost:5173
```

If you open the UI as `http://127.0.0.1:5173`, add that origin too.

The same Google client id must be used by:

1. demo UI as `VITE_GOOGLE_CLIENT_ID`;
2. booking-service JWT verifier as the accepted audience.

## Demo flow

### Step 1 — Sign in

Open the demo UI and sign in with Google.

Expected result:

```text
Google auth
<user name>
<user email>
Sign out
```

### Step 2 — Prepare inventory

Open `Inventory Admin`.

Create a hotel, for example:

```text
Hotel name: Demo Hotel
City: Kazan
```

Add a room type, for example:

```text
Room type: Standard
Guest capacity: 2
```

Initialize availability, for example:

```text
From: 2030-06-11
To:   2030-06-20
Total rooms: 10
```

### Step 3 — Check availability

Open `Hotels`.

Select the created hotel and room type, then check availability for the initialized dates.

Expected result:

```text
availableRooms = totalRooms - heldRooms - bookedRooms
```

### Step 4 — Start happy-path booking saga

Use payment amount below or equal to `50000`.

Expected result:

```text
Saga accepted
Inventory hold created
Payment authorized
Booking confirmed
```

Exact timing may be asynchronous. Use `My bookings` and `Timeline` to observe the final state.

### Step 5 — Start compensation scenario

Use payment amount above `50000`.

Expected result:

```text
Payment declined
Saga compensated
Inventory hold released
Booking rejected/cancelled depending on current domain rules
```

This demonstrates compensation behavior.

### Step 6 — Timeline

Open `My bookings`, select `Timeline`.

Expected events include booking and payment lifecycle events recorded by `audit-service`.

### Step 7 — Cancellation

For active bookings, click `Cancel`.

Expected result:

```text
Booking status changes
Audit timeline receives cancellation event
```

## Troubleshooting

### 401 Unauthorized from booking-service

Most common cause: auth mode mismatch.

| UI mode | booking-service security profile |
|---|---|
| `google` | `security-jwt` through `dev-jwt` |
| `demo` | `security-dev` through `dev` |

### Google error: `no registered origin`

The current browser origin is not allowed in the Google OAuth client.

Add exactly the origin used by the browser:

```text
http://localhost:5173
```

### Google button language

The UI requests English for the Google button, but browser/account locale may still influence the rendered Google Identity Services widget in some environments.

### Color logs disappear with `dev-jwt`

Check shared Logback configuration.

The colored profile block should include:

```xml
<springProfile name="dev | dev-jwt | local | local-kafka | local-jwt | local-jwt-kafka">
```

The production-like profile must not be just `!dev`, because `dev-jwt` also satisfies `!dev`. Use a full exclusion expression instead:

```xml
<springProfile name="!dev &amp; !dev-jwt &amp; !local &amp; !local-kafka &amp; !local-jwt &amp; !local-jwt-kafka">
```
