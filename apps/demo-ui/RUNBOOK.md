# Demo UI Runbook

This runbook demonstrates the end-to-end backend flow through the React UI.

## 1. Start backend

Recommended Google-authenticated demo:

| Service | Profile |
|---|---|
| `inventory-service` | `dev` |
| `payment-service` | `dev` |
| `notification-service` | `dev` |
| `audit-service` | `dev` |
| `booking-service` | `dev-jwt` |

Start local infrastructure first:

```bash
docker compose up -d
```

Generate mTLS certificates once if needed:

```bash
./scripts/generate-dev-mtls-certs.sh
```

## 2. Start UI

```bash
cd apps/demo-ui
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

## 3. Sign in

Use the Google auth panel in the top right corner.

After sign-in, booking requests are sent with:

```http
Authorization: Bearer <google-id-token>
```

## 4. Prepare inventory

Open `Inventory Admin`:

1. Create hotel.
2. Add room type.
3. Initialize availability.

Recommended demo dates:

```text
2030-06-11 -> 2030-06-20
```

## 5. Check availability

Open `Hotels`:

1. Select hotel.
2. Select room type.
3. Choose date range.
4. Click `Check availability`.

## 6. Start booking saga

In `Hotels`, fill booking form and click `Start booking saga`.

Payment rule used by the fake provider:

| Payment amount | Expected result |
|---:|---|
| `<= 50000` | payment authorization succeeds; booking can be confirmed |
| `> 50000` | payment is declined; saga compensation releases inventory and cancels booking |

## 7. Inspect booking state

Open `My bookings`:

- verify status;
- open `Timeline`;
- inspect booking/payment events;
- cancel active booking if needed.

## 8. Troubleshooting

| Symptom | Check |
|---|---|
| Google button is missing | `VITE_AUTH_MODE=google` and `VITE_GOOGLE_CLIENT_ID` are set; Vite was restarted. |
| `no registered origin` | Google OAuth client allows `http://localhost:5173`. |
| booking-service returns `401` | UI auth mode and booking-service profile match. |
| demo mode returns `401` | Run booking-service with `dev`, not `dev-jwt`. |
| Google mode returns `401` | Run booking-service with `dev-jwt` and check JWT audience/client id. |

