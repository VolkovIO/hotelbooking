# Security Model

## Purpose

The project uses security to answer four different questions:

```text
Authentication
  Who is the caller?

Authorization
  What is the caller allowed to do?

Ownership
  Does this booking belong to this user?

Service trust
  How do internal services communicate?
```

The current security scope is focused on the booking and inventory service applications.

## Current services

```text
booking-service-app
inventory-service-app
```

Booking communicates with inventory through gRPC.

The user-facing security boundary is currently HTTP API security.

The gRPC service-to-service boundary is still trusted/internal and will be hardened later.

## Security profiles

The project supports two security profiles:

```text
security-dev
security-jwt
```

Only one of them should be active at the same time.

## security-dev

`security-dev` is the default local development profile.

It is used for local Swagger testing without a real JWT token.

Example default booking service profiles:

```yaml
spring:
  profiles:
    active:
      - booking-postgres
      - inventory-grpc-client
      - security-dev
```

In this mode:

```text
DevSecurityAuthenticationFilter
  creates an authenticated Spring Security user

DevCurrentUserProvider
  returns a local development user

The development user has USER and ADMIN privileges
```

This allows local testing through Swagger without Google OAuth2 setup.

## security-jwt

`security-jwt` is the production-like profile.

Run booking service in JWT mode:

```bash
./gradlew :apps:booking-service-app:bootRun --args="--spring.profiles.active=booking-postgres,inventory-grpc-client,security-jwt"
```

On Windows:

```bash
gradlew.bat :apps:booking-service-app:bootRun --args="--spring.profiles.active=booking-postgres,inventory-grpc-client,security-jwt"
```

Run inventory service in JWT mode:

```bash
./gradlew :apps:inventory-service-app:bootRun --args="--spring.profiles.active=inventory-mongo,inventory-grpc-server,security-jwt"
```

On Windows:

```bash
gradlew.bat :apps:inventory-service-app:bootRun --args="--spring.profiles.active=inventory-mongo,inventory-grpc-server,security-jwt"
```

In this mode protected endpoints require:

```http
Authorization: Bearer <JWT>
```

JWT configuration is stored in:

```text
application-security-jwt.yaml
```

Example:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://accounts.google.com
```

Spring Security validates JWT tokens using Google OpenID Connect metadata and public keys.

## Why fake JWT tokens do not work

A JWT is not accepted just because it has a valid-looking structure.

In `security-jwt` mode, the service validates:

```text
issuer
signature
public key
expiration
claims
```

A manually created token such as:

```text
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

is rejected because it was not issued and signed by Google.

Expected behavior:

```text
No token
  -> 401 Unauthorized

Fake token
  -> 401 Unauthorized

Valid Google JWT with USER role
  -> allowed for USER endpoints
  -> forbidden for ADMIN endpoints
```

## Local user model

The project uses a local user account model.

External identity providers should not leak directly into the booking domain.

Current mapping:

```text
Google JWT
  -> provider = GOOGLE
  -> provider_subject = JWT subject
  -> local app_user
  -> internal UserId
  -> Booking.userId
```

Booking stores only internal `UserId`.

Booking does not store Google subject, Google email or OAuth2 provider details directly.

## Booking ownership

Booking has an owner:

```text
Booking.userId
```

The request body does not accept `userId`.

This is intentional.

Bad API design:

```json
{
  "userId": "...",
  "hotelId": "...",
  "roomTypeId": "..."
}
```

Good API design:

```text
userId comes from authenticated security context
```

The booking service checks ownership in application use cases.

Example rule:

```text
Only the owner of a booking can read, confirm or cancel that booking.
```

This rule is not implemented only at URL level.

Spring Security checks coarse-grained access:

```text
Is the caller authenticated?
Does the caller have USER or ADMIN role?
```

The booking application layer checks fine-grained ownership:

```text
Does this booking belong to this user?
```

## Roles

Current roles:

```text
USER
ADMIN
```

USER can:

```text
create booking
view own booking
confirm own booking
cancel own booking
```

ADMIN can:

```text
access admin endpoints
manage inventory
```

Current limitation:

```text
Google JWT does not contain project-specific ADMIN role.
```

Therefore, project roles are stored in local `app_users`.

## Booking service authorization

Booking service protects booking operations.

Examples:

```text
POST /api/v1/bookings
  requires USER or ADMIN

GET /api/v1/bookings/{bookingId}
  requires USER or ADMIN
  also requires booking ownership

POST /api/v1/bookings/{bookingId}/confirm
  requires USER or ADMIN
  also requires booking ownership

POST /api/v1/bookings/{bookingId}/cancel
  requires USER or ADMIN
  also requires booking ownership
```

## Inventory service authorization

Inventory service protects admin endpoints.

Examples:

```text
/api/v1/admin/**
  requires ADMIN
```

Public inventory read endpoints can remain open.

Examples:

```text
hotel search
room type reference lookup
availability query
```

## Service-to-service communication

Booking calls inventory through gRPC.

Current state:

```text
booking-service-app
  -> inventory-service-app gRPC
```

The gRPC boundary is currently trusted/internal.

Inventory does not know who owns a booking.

This is intentional.

Booking owns booking authorization and ownership checks.

Inventory owns inventory rules:

```text
hotel exists
room type exists
availability exists
hold can be placed
hold can be confirmed
hold can be released
booked rooms can be released
```

Future improvements:

```text
service-to-service token
mTLS
service mesh
gRPC metadata propagation
```

## Why inventory does not check booking ownership

Inventory is a separate bounded context.

It should not decide:

```text
Can this user cancel this booking?
```

That rule belongs to booking.

Inventory decides:

```text
Can this room hold be released?
Can this confirmed reservation be cancelled from inventory?
```

## Production-like direction

The intended production-like model is:

```text
Frontend
  -> Google login / OIDC
  -> receives JWT / ID token
  -> calls booking-service with Bearer token

booking-service
  -> validates JWT
  -> maps external identity to local app_user
  -> uses internal UserId
  -> checks booking ownership
  -> calls inventory through internal gRPC

inventory-service
  -> protects admin HTTP endpoints
  -> exposes internal gRPC operations
```

## Current limitations

The current implementation is still educational.

Known limitations:

```text
No frontend login flow yet
No refresh token flow yet
No service-to-service authentication yet
No mTLS yet
No API gateway / BFF yet
Admin role management is minimal
Google OAuth2 setup is not fully automated
```

## Future work

Planned improvements:

```text
Frontend login with Google
Swagger OAuth2 authorization support
Role administration
Service-to-service authentication
Correlation ID propagation
Audit logging
Security tests
```
