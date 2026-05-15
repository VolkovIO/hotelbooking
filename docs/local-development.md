# Local Development

This document describes the recommended local development setup for `v0.14.0`.

The main goal is to make a freshly cloned repository easy to run while keeping the local setup close enough to real service boundaries: separate applications, PostgreSQL, MongoDB, Kafka, gRPC and mTLS.

## Java and Gradle

Use the Gradle wrapper from the repository root.

Common commands:

```bash
./gradlew check
./gradlew clean check
./gradlew spotlessApply
```

On Windows, use:

```bat
gradlew.bat check
```

## Required free ports

Before starting Docker Compose and the applications, make sure these host ports are not already used.

### Application ports

| Application | Port | Notes |
|---|---:|---|
| booking-service | 8080 | main booking API, Swagger, Actuator |
| inventory-service | 8081 | inventory HTTP API, Swagger, Actuator |
| notification-service | 8082 | notification API, Actuator |
| payment-service | 8083 | payment API, Swagger, Actuator |
| audit-service | 8084 | timeline API, Swagger, Actuator |
| inventory gRPC server | 9090 | internal booking -> inventory gRPC calls |

### Docker Compose ports

| Container | Port | Notes |
|---|---:|---|
| PostgreSQL | 5432 | booking and payment logical databases |
| MongoDB | 27017 | inventory, audit and notification data |
| Kafka | 9092 | local broker exposed to host applications |
| Kafka UI | 8089 | browser UI for local Kafka topics |

Kafka also uses internal container listeners such as `29092` and `9093`, but those are not exposed as host ports in the local compose file.

## Start local infrastructure

From the repository root:

```bash
docker compose up -d
```

Typical containers:

- PostgreSQL
- MongoDB
- Kafka
- Kafka UI

Stop infrastructure without deleting data:

```bash
docker compose down
```

Reset local infrastructure data:

```bash
docker compose down -v
```

Use reset carefully: it deletes local PostgreSQL, MongoDB and Kafka volumes.

## PostgreSQL databases

Local PostgreSQL uses one container with separate logical databases:

```text
hotelbooking          -> booking-service
hotelbooking_payment  -> payment-service
```

This keeps service data ownership clear without requiring multiple PostgreSQL containers for local development.

## Local mTLS certificates

Booking-service communicates with inventory-service through gRPC. In the local `dev` setup, inventory gRPC TLS/mTLS is enabled by default.

Generate certificates once after cloning the repository:

```bash
./scripts/generate-dev-mtls-certs.sh
```

On Windows, run it from Git Bash or WSL. The script requires `openssl`.

Generated files:

```text
certs/dev/ca.crt
certs/dev/inventory-service.crt
certs/dev/inventory-service.key
certs/dev/booking-service.crt
certs/dev/booking-service.key
```

The generated files are ignored by Git. The repository keeps only `certs/dev/.gitkeep`.

The default configuration expects these files:

```text
inventory.grpc.server.tls.certificate-chain=certs/dev/inventory-service.crt
inventory.grpc.server.tls.private-key=certs/dev/inventory-service.key
inventory.grpc.server.tls.trust-cert-collection=certs/dev/ca.crt

inventory.grpc.client.tls.certificate-chain=certs/dev/booking-service.crt
inventory.grpc.client.tls.private-key=certs/dev/booking-service.key
inventory.grpc.client.tls.trust-cert-collection=certs/dev/ca.crt
```

If certificates are missing and TLS is enabled, inventory-service or booking-service will fail during startup. That is expected and is safer than silently running without service identity.

For temporary local troubleshooting only, TLS can be disabled with environment variables:

```text
INVENTORY_GRPC_SERVER_TLS_ENABLED=false
INVENTORY_GRPC_CLIENT_TLS_ENABLED=false
```

The preferred demo path is to generate certificates and keep mTLS enabled.

## Demo inventory data

The project contains a MongoDB demo data script:

```text
docker/mongo/init/demo-data.js
```

It creates demo hotels, room types and availability for:

```text
2030-06-01 .. 2030-06-30
```

Important fixed ids used in demo requests:

| Item | UUID |
|---|---|
| Demo Kazan Hotel | `10000000-0000-0000-0000-000000000001` |
| Kazan STANDARD | `20000000-0000-0000-0000-000000000001` |
| Kazan FAMILY | `20000000-0000-0000-0000-000000000002` |
| Kazan LUX | `20000000-0000-0000-0000-000000000003` |
| Demo Almetyevsk Hotel | `10000000-0000-0000-0000-000000000002` |
| Almetyevsk STANDARD | `20000000-0000-0000-0000-000000000004` |
| Almetyevsk BUSINESS | `20000000-0000-0000-0000-000000000005` |
| Demo Naberezhnye Chelny Hotel | `10000000-0000-0000-0000-000000000003` |
| Chelny ECONOMY | `20000000-0000-0000-0000-000000000006` |
| Chelny STANDARD | `20000000-0000-0000-0000-000000000007` |

### Option A: run the Mongo script manually

This works even if MongoDB already has an existing volume:

```bash
docker cp docker/mongo/init/demo-data.js hotelbooking-mongo:/tmp/demo-data.js
docker exec hotelbooking-mongo mongosh /tmp/demo-data.js
```

The script uses upsert operations for hotels and availability and clears demo holds for demo hotels.

### Option B: run Mongo init script automatically on a fresh volume

MongoDB executes scripts from `/docker-entrypoint-initdb.d` only when the database volume is created for the first time.

If the compose file mounts the init directory, for example:

```yaml
mongo:
  volumes:
    - hotelbooking_mongo_data:/data/db
    - ./docker/mongo/init:/docker-entrypoint-initdb.d:ro
```

then reset the Mongo volume and start infrastructure again:

```bash
docker compose down -v
docker compose up -d mongo
```

Use this option carefully because `down -v` removes all local compose volumes.

### Option C: create inventory through Swagger

Inventory admin endpoints can also create demo data manually:

```http
POST /api/v1/admin/hotels
POST /api/v1/admin/hotels/{hotelId}/room-types
POST /api/v1/admin/hotels/{hotelId}/room-types/{roomTypeId}/availability/initialization
```

With `security-dev`, inventory admin endpoints are available through Swagger as a local demo administrator.

This option is useful for understanding the API, but the Mongo script is faster for repeatable demos because it uses fixed ids.

## Recommended profile

Use:

```text
--spring.profiles.active=dev
```

The `dev` profile is the preferred local profile group for the current milestone.

It should enable:

- local persistence
- Kafka integration
- dev security where applicable
- outbox publishers
- logging notification sender
- observability configuration
- inventory gRPC TLS/mTLS

Older `local` profiles may exist for compatibility, but new documentation and demo scripts should prefer `dev`.

## Local dev security

### Booking-service dev user

When booking-service runs with `security-dev`, the current user is automatically resolved as:

```text
provider: DEV
subject:  dev-user
email:    dev@example.com
name:     Development User
roles:    USER, ADMIN
```

This user is created or reused in the booking database through the same internal account mapping used by the JWT path.

In local `dev`, all booking requests are effectively made by this same demo user. This makes Swagger testing easy and deterministic.

### Booking ownership

Bookings are owned by an internal application `UserId`, not directly by an email address.

The important rule is:

```text
regular users can access only their own bookings
```

The application layer checks ownership against the current internal user. In `security-dev`, that user is always the demo user above. In `security-jwt`, the user is resolved from Google JWT claims.

### Google JWT profile

Booking-service also supports a Google JWT resource-server profile:

```text
security-jwt
```

In that mode:

```text
Google JWT subject/email/name
  -> app_users
  -> internal UserId
  -> Booking.userId
```

A newly seen Google account is created as a normal `USER`. Admin role support is represented in the account model but is not the focus of the local demo flow.

The local `dev` profile does not require a real Google login.

## Starting services

Start each service with the `dev` profile.

Inventory:

```bash
./gradlew :apps:inventory-service-app:bootRun --args="--spring.profiles.active=dev"
```

Payment:

```bash
./gradlew :apps:payment-service-app:bootRun --args="--spring.profiles.active=dev"
```

Notification:

```bash
./gradlew :apps:notification-service-app:bootRun --args="--spring.profiles.active=dev"
```

Audit:

```bash
./gradlew :apps:audit-service-app:bootRun --args="--spring.profiles.active=dev"
```

Booking:

```bash
./gradlew :apps:booking-service-app:bootRun --args="--spring.profiles.active=dev"
```

Recommended order:

```text
1. docker compose up -d
2. generate mTLS certs if not generated yet
3. initialize demo inventory data
4. start inventory-service
5. start payment-service
6. start notification-service
7. start audit-service
8. start booking-service
```

## Swagger UI

| Service | Swagger URL |
|---|---|
| booking-service | `http://localhost:8080/swagger-ui.html` |
| inventory-service | `http://localhost:8081/swagger-ui.html` |
| payment-service | `http://localhost:8083/swagger-ui.html` |
| audit-service | `http://localhost:8084/swagger-ui.html` |

Notification-service may expose APIs depending on current profiles and controllers.

## Actuator health

```http
GET http://localhost:8080/actuator/health
GET http://localhost:8081/actuator/health
GET http://localhost:8082/actuator/health
GET http://localhost:8083/actuator/health
GET http://localhost:8084/actuator/health
```

Readiness and liveness:

```http
GET /actuator/health/readiness
GET /actuator/health/liveness
```

## Useful environment variables

Kafka:

```text
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
BOOKING_EVENTS_TOPIC=booking.events
PAYMENT_EVENTS_TOPIC=payment.events
```

Booking -> payment:

```text
PAYMENT_SERVICE_BASE_URL=http://localhost:8083
PAYMENT_SERVICE_RESPONSE_TIMEOUT=5s
```

Booking -> inventory gRPC:

```text
INVENTORY_GRPC_HOST=localhost
INVENTORY_GRPC_PORT=9090
INVENTORY_GRPC_CLIENT_DEADLINE=PT3S
INVENTORY_GRPC_CLIENT_TLS_ENABLED=true
INVENTORY_GRPC_SERVER_TLS_ENABLED=true
```

Payment fake provider:

```text
PAYMENT_SERVICE_PORT=8083
```

Notification delivery:

```text
NOTIFICATION_DELIVERY_FIXED_DELAY_MS=30000
NOTIFICATION_DELIVERY_BATCH_SIZE=20
```

## Recommended checks before PR

Fast module checks during development:

```bash
./gradlew :modules:booking:test
./gradlew :modules:payment:test
./gradlew :modules:notification:test
```

Application compile checks:

```bash
./gradlew :apps:booking-service-app:compileJava
./gradlew :apps:payment-service-app:compileJava
./gradlew :apps:notification-service-app:compileJava
./gradlew :apps:audit-service-app:compileJava
./gradlew :apps:inventory-service-app:compileJava
```

Full check:

```bash
./gradlew clean check
```
