# Security and Service Identity

This document describes the security model used by the project in `v0.14.0`.

The project intentionally separates two concerns:

```text
External user authentication
  User / frontend -> booking-service HTTP API

Internal service authentication
  booking-service -> inventory-service gRPC API
```

## Summary

| Boundary | Current mechanism | Purpose |
|---|---|---|
| Local demo user -> booking-service | `security-dev` mock current user | easy Swagger/demo usage |
| Google-authenticated user -> booking-service | `security-jwt` resource server | production-like external identity path |
| booking-service -> inventory-service | gRPC TLS/mTLS | internal service identity |
| inventory HTTP admin endpoints | `security-dev` local admin | demo data setup |

## Local dev user

With `security-dev`, booking-service resolves every request as one development user:

```text
provider: DEV
subject:  dev-user
email:    dev@example.com
name:     Development User
roles:    USER, ADMIN
```

This is intentionally simple and is used only for local development and portfolio demos.

The user is created or reused in the booking database through the normal account mapping path. This means bookings are still attached to an internal application `UserId`, even in dev mode.

## Booking ownership

Booking ownership is enforced against the internal application user id:

```text
CurrentUser.id -> Booking.userId
```

A regular user can access only their own bookings. This rule belongs to the booking application layer and does not depend on whether the current user came from `security-dev` or Google JWT.

In local `dev`, all booking requests use the same demo user, so all demo bookings belong to that user.

## Google JWT profile

Booking-service supports Google JWT authentication in the `security-jwt` profile.

The mapping is:

```text
Google JWT subject/email/name
  -> app_users
  -> internal UserId
  -> Booking.userId
```

A newly seen Google account is created as a normal `USER`.

The configured issuer is:

```text
https://accounts.google.com
```

The local demo does not require a real Google login. The default demo path uses `dev` / `security-dev`.

## Inventory service identity with mTLS

Booking-service talks to inventory-service through gRPC.

The local mTLS setup uses:

```text
local CA                    -> certs/dev/ca.crt
inventory server cert       -> CN=inventory-service
booking client cert         -> CN=booking-service
```

Inventory gRPC server:

- presents the `inventory-service` server certificate
- trusts the local development CA
- requires a client certificate
- accepts the `booking-service` client identity

Booking gRPC client:

- presents the `booking-service` client certificate
- trusts the local development CA
- calls inventory on `localhost:9090`

Generate local certs:

```bash
./scripts/generate-dev-mtls-certs.sh
```

The generated keys and certificates are local-only and ignored by Git.

## Why not forward Google JWT to inventory?

The project treats external user identity and internal service identity as separate problems.

Booking-service owns the user-facing authorization decision:

```text
Can this user create/read/cancel this booking?
```

Inventory-service receives internal reservation commands from booking-service. It should authenticate the calling service, not the end user. For this boundary, mTLS is a better fit than forwarding browser/user JWTs.

## Inventory admin endpoints

Inventory HTTP admin endpoints exist for local/demo data setup:

```http
POST /api/v1/admin/hotels
POST /api/v1/admin/hotels/{hotelId}/room-types
POST /api/v1/admin/hotels/{hotelId}/room-types/{roomTypeId}/availability/initialization
PUT  /api/v1/admin/hotels/{hotelId}/room-types/{roomTypeId}/availability/capacity
```

With `security-dev`, these endpoints are available as a development administrator.

This is not intended as a production-grade admin security model.

## Important trade-offs

Current intentional boundaries:

- no production certificate rotation model yet
- no SPIFFE/SPIRE integration yet
- no real frontend login flow in the local demo
- inventory admin authentication is local/demo-oriented
- Google JWT is implemented for booking-service, but the standard portfolio demo uses `security-dev`

These trade-offs are acceptable for the current milestone because the project focuses on service boundaries, ownership checks, mTLS, saga flow and observability.
