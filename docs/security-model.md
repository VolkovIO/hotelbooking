# Security Model

## Purpose

This document describes the current and target security model for the hotel booking project.

The project intentionally separates two concerns:

```text
External user authentication
  User / Frontend -> Booking HTTP API

Internal service authentication
  Booking service -> Inventory gRPC API
```

These concerns should not be solved by the same mechanism.

---

## Target security model

The target model is:

```text
External user access:
  User / Frontend
    -> Booking HTTP API
    -> Google JWT

Internal service access:
  Booking service
    -> Inventory gRPC API
    -> mTLS
```

Google JWT is used to identify the external user and map that user to an internal application user.

mTLS is the target mechanism for internal booking-to-inventory communication.

Booking should not forward user JWT tokens to inventory as the main service-to-service authentication mechanism.

---

## Current state

The booking service supports Google JWT authentication in the `security-jwt` profile.

The booking service maps an authenticated Google user to an internal application user and uses the internal user id for booking ownership checks.

The inventory service still contains HTTP security profiles from earlier learning stages:

```text
security-dev
security-jwt
```

The inventory HTTP API is used for public catalog browsing and administrative data setup.

The booking-to-inventory gRPC boundary is not yet protected by mTLS. This is planned for a later release.

---

## Public browsing before login

The user should be able to browse hotels, room types and availability before logging in.

Public inventory catalog endpoints:

```text
GET /api/v1/hotels
GET /api/v1/hotels/{hotelId}
GET /api/v1/hotels/{hotelId}/room-types/{roomTypeId}/availability
```

These endpoints are intentionally public.

Booking creation is different:

```text
POST /api/v1/bookings
```

Creating a booking requires an authenticated user because booking ownership must be assigned to an internal `UserId`.

The intended user journey is:

```text
browse hotels and availability anonymously
  -> choose room type and stay period
  -> login with Google
  -> create booking
  -> manage own booking
```

---

## Booking ownership

Bookings belong to internal application users, not directly to Google accounts.

The intended mapping is:

```text
Google JWT subject/email
  -> app_users
  -> internal UserId
  -> Booking.userId
```

Booking ownership checks are enforced by the booking application layer.

A user should be able to access only their own bookings unless they have an administrative role.

---

## HTTP security rules

### Booking HTTP API

Public endpoints:

```text
GET /swagger-ui.html
GET /swagger-ui/**
GET /v3/api-docs/**
GET /v3/api-docs.yaml
```

Protected endpoints:

```text
POST /api/v1/bookings
GET  /api/v1/bookings/**
POST /api/v1/bookings/**
PUT  /api/v1/bookings/**
DELETE /api/v1/bookings/**
```

Booking endpoints require:

```text
ROLE_USER or ROLE_ADMIN
```

Any unmatched booking HTTP request should be denied by default.

### Inventory HTTP API

Public endpoints:

```text
GET /swagger-ui.html
GET /swagger-ui/**
GET /v3/api-docs/**
GET /v3/api-docs.yaml

GET /api/v1/hotels
GET /api/v1/hotels/**
```

Administrative endpoints:

```text
POST /api/v1/admin/hotels
POST /api/v1/admin/hotels/{hotelId}/room-types
POST /api/v1/admin/hotels/{hotelId}/room-types/{roomTypeId}/availability/initialization
PUT  /api/v1/admin/hotels/{hotelId}/room-types/{roomTypeId}/availability/capacity
```

Administrative endpoints require:

```text
ROLE_ADMIN
```

Any unmatched inventory HTTP request should be denied by default.

---

## Development admin access

Inventory administrative HTTP endpoints are protected by `ROLE_ADMIN`.

In local development, the `security-dev` profile installs a development authentication filter.
This filter automatically authenticates requests as a mock administrator:

```text
username: dev-admin
roles: ROLE_USER, ROLE_ADMIN
```

This is intended only for local development and demo data setup through Swagger.

Example local inventory startup:

```bash
./gradlew :apps:inventory-service-app:bootRun --args="--spring.profiles.active=local"
```

When the local profile includes `security-dev`, inventory admin endpoints can be used from Swagger without a real login flow.

This is not a production-grade admin authentication model.

---

## Inventory JWT profile

The inventory service still contains an HTTP JWT security profile from an earlier learning stage.

This profile is transitional and is not the target mechanism for booking-to-inventory communication.

Target model:

```text
booking-service -> inventory-service gRPC -> mTLS
```

The inventory HTTP API keeps public catalog endpoints open and protects administrative endpoints.

A production-grade inventory admin authentication model is intentionally not implemented yet.

---

## Planned mTLS model

The planned internal service security model is:

```text
booking-service-app
  has client certificate

inventory-service-app
  has server certificate
  trusts the internal CA
  requires client certificate
  accepts calls only from allowed service identities
```

Potential service identity examples:

```text
CN=booking-service
SAN DNS=booking-service
SAN URI=spiffe://hotelbooking/booking-service
```

For the educational version, a simple internal CA and service certificates are enough.

SPIFFE/SPIRE can be considered later, but is not required for the first mTLS implementation.

---

## Inventory gRPC authorization rule

Inventory gRPC operations should allow only trusted internal services.

Initial rule:

```text
booking-service may call inventory reservation and lookup gRPC methods
unknown service identities are rejected
```

The inventory service should authenticate the calling service by its client certificate.

---

## Future work

Planned for `v0.6.2`:

```text
- generate local development CA
- generate booking service client certificate
- generate inventory service server certificate
- configure inventory gRPC server to require client certificates
- configure booking gRPC client with client certificate and trust store
- validate booking-service identity from the client certificate
- document local certificate generation
- add a negative test for rejected unauthenticated gRPC clients
```

Later improvements:

```text
- production-grade inventory admin authentication
- service identity documentation
- certificate rotation strategy
- optional SPIFFE/SPIRE evaluation
- audit events for security-sensitive actions
```
