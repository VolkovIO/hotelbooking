# Hotel Booking Demo UI

Minimal React + Vite + TypeScript UI for demonstrating the local `hotelbooking` platform.

The UI is intentionally thin. Its purpose is not to replace backend APIs, but to make the distributed booking flow easy to show during a demo:

- hotel catalog and room availability from `inventory-service`;
- booking saga creation through `booking-service`;
- Google-authenticated current user booking list;
- booking cancellation;
- audit timeline from `audit-service`;
- simple inventory admin operations for local demo data.

## Runtime service routes

The UI talks to backend services through Vite proxy routes:

| UI route | Backend service | Default local port |
|---|---:|---:|
| `/booking-api` | `booking-service` | `8080` |
| `/inventory-api` | `inventory-service` | `8081` |
| `/audit-api` | `audit-service` | `8084` |

The routes are configured through environment variables and proxied by Vite during local development.

## Environment files

This application uses Vite environment variables.

Recommended setup:

| File | Committed | Purpose |
|---|---:|---|
| `.env` | yes | Default project settings for local demo. In this project it enables Google auth by default. |
| `.env.example` | yes | Template and documentation for supported variables. |
| `.env.local` | no | Personal local overrides. Use it to switch to `demo` mode without changing committed files. |

All variables exposed to the frontend must start with `VITE_`.

`VITE_GOOGLE_CLIENT_ID` is a public browser OAuth client id. It is not a secret. Do not put OAuth client secrets into frontend environment files.

## Auth modes

### Google mode

Default mode for the demo UI.

```env
VITE_AUTH_MODE=google
VITE_GOOGLE_CLIENT_ID=<google-web-client-id>.apps.googleusercontent.com
```

In this mode:

1. the browser signs in with Google;
2. Google returns an ID token;
3. the UI sends the token to `booking-service` as:

```http
Authorization: Bearer <google-id-token>
```

`booking-service` must be started with a JWT security profile, for example:

```text
spring.profiles.active=dev-jwt
```

The Google client id used by the UI must match the JWT audience accepted by `booking-service`.

The Google OAuth client must allow the local JavaScript origin:

```text
http://localhost:5173
```

If the UI is opened through a different origin, for example `http://127.0.0.1:5173`, that origin must also be added in Google Cloud Console.

### Demo mode

Use demo mode when you want to run the UI without Google sign-in.

Create a local override file:

```text
apps/demo-ui/.env.local
```

with:

```env
VITE_AUTH_MODE=demo
```

Then restart Vite.

In demo mode the UI does not send an `Authorization` header. `booking-service` must be started with the dev security profile, for example:

```text
spring.profiles.active=dev
```

The backend will use the demo user configured by the service, currently `dev@example.com`.

## Local start

Install dependencies once:

```bash
cd apps/demo-ui
npm install
```

Start the Vite dev server:

```bash
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

## Demo scenario

### 1. Create or verify inventory data

Open `Inventory Admin`:

1. create a hotel;
2. add a room type;
3. initialize availability for demo dates.

Default demo dates are usually in the future, for example `2030-06-11` to `2030-06-20`.

### 2. Check availability

Open `Hotels`:

1. select a hotel;
2. select a room type;
3. choose date range;
4. click `Check availability`.

### 3. Start booking saga

In `Hotels`, use `Create booking`:

1. choose guest count;
2. choose payment amount;
3. choose saga engine;
4. click `Start booking saga`.

Demo payment rule:

| Payment amount | Expected flow |
|---:|---|
| `<= 50000` | happy path / payment authorization succeeds |
| `> 50000` | payment decline / compensation flow |

### 4. View booking status and timeline

Open `My bookings`:

1. inspect current user bookings;
2. open `Timeline` for a booking;
3. verify events from `audit-service`;
4. cancel active booking if needed.

## Troubleshooting

### Google button is not shown

Check that `apps/demo-ui/.env` or `apps/demo-ui/.env.local` contains:

```env
VITE_AUTH_MODE=google
VITE_GOOGLE_CLIENT_ID=<google-web-client-id>.apps.googleusercontent.com
```

Restart Vite after changing env files.

### Google sign-in fails with `no registered origin`

Add the local Vite origin to the OAuth client in Google Cloud Console:

```text
http://localhost:5173
```

The origin must match exactly how you open the UI.

### Booking API returns 401

Check that UI auth mode and `booking-service` security profile match:

| UI mode | booking-service profile |
|---|---|
| `google` | `dev-jwt` or another JWT profile |
| `demo` | `dev` or another dev security profile |

`google` UI mode sends a bearer token. `demo` UI mode does not.

### Environment changes do not apply

Vite reads env files at startup. Stop and restart the dev server.

### Logs are not colored with `dev-jwt`

Make sure the shared Logback profile configuration treats `dev-jwt` as a local/dev profile. The production-like `!dev` block must not override the colored pattern for `dev-jwt`.
