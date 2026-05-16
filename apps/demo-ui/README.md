# Hotel Booking Demo UI

Thin React + Vite + TypeScript UI for demonstrating the local Hotel Booking backend flow.

The UI is not the main product. It is a presentation layer for the backend case study: inventory availability, booking saga, Google-authenticated user bookings, cancellation and audit timeline.

## Runtime routes

During local development Vite proxies frontend routes to backend services:

| UI route | Backend service | Default port |
|---|---|---:|
| `/booking-api` | `booking-service` | `8080` |
| `/inventory-api` | `inventory-service` | `8081` |
| `/audit-api` | `audit-service` | `8084` |

## Environment files

| File | Commit? | Purpose |
|---|---:|---|
| `.env` | yes | Default project settings. Google auth is enabled by default. |
| `.env.example` | yes | Template and explanation. |
| `.env.local` | no | Personal overrides, for example switching to demo auth. |

`VITE_GOOGLE_CLIENT_ID` is a public browser OAuth client id, not a secret.

## Google auth mode

Default mode:

```env
VITE_AUTH_MODE=google
```

Flow:

1. User signs in with Google.
2. Google returns an ID token.
3. UI sends it to booking-service:

```http
Authorization: Bearer <google-id-token>
```

Required backend profile:

```text
booking-service: dev-jwt
```

Google OAuth client must allow:

```text
http://localhost:5173
```

## Demo auth mode

Create local override:

```text
apps/demo-ui/.env.local
```

```env
VITE_AUTH_MODE=demo
```

Restart Vite. In this mode the UI does not send an Authorization header.

Required backend profile:

```text
booking-service: dev
```

## Start

```bash
cd apps/demo-ui
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

Build check:

```bash
npm run build
```

## Demo runbook

See [`RUNBOOK.md`](RUNBOOK.md).

