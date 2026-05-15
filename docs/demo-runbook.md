# Demo Runbook

This runbook describes a compact local demo flow for the Hotel Booking project.

It is written for `v0.14.0`, where the recommended local profile is:

```text
--spring.profiles.active=dev
```

The purpose of the demo is to show the full distributed booking flow and the observability signals around it.


## 0. Prerequisites

Before starting the demo:

1. Make sure required host ports are free: `5432`, `27017`, `9092`, `9090`, `8080`-`8084`, `8089`.
2. Generate local mTLS certificates if `certs/dev/*.crt` and `certs/dev/*.key` are missing:

```bash
./scripts/generate-dev-mtls-certs.sh
```

3. Start Docker Compose:

```bash
docker compose up -d
```

4. Load demo inventory data if MongoDB was not initialized with it yet:

```bash
docker cp docker/mongo/init/demo-data.js hotelbooking-mongo:/tmp/demo-data.js
docker exec hotelbooking-mongo mongosh /tmp/demo-data.js
```

Local booking requests run as the dev user:

```text
dev@example.com
```

This is expected when `--spring.profiles.active=dev` is used.

The demo booking requests below use the fixed Kazan demo hotel and room type:

```text
hotelId:    10000000-0000-0000-0000-000000000001
roomTypeId: 20000000-0000-0000-0000-000000000001
```

The complete happy-path request body is:

```json
{
  "hotelId": "10000000-0000-0000-0000-000000000001",
  "roomTypeId": "20000000-0000-0000-0000-000000000001",
  "checkIn": "2030-06-11",
  "checkOut": "2030-06-12",
  "guestCount": 1,
  "paymentAmount": 1500,
  "paymentCurrency": "RUB"
}
```

The fake payment provider has a deterministic demo rule:

```text
paymentAmount <= 50000  -> payment authorization succeeds
paymentAmount > 50000   -> payment authorization is declined
```

This makes it possible to demonstrate both a successful saga and a payment-decline scenario without calling a real payment provider.

## 1. Start local infrastructure

From the project root:

```bash
docker compose up -d
```

Typical infrastructure:

- PostgreSQL
- MongoDB
- Kafka
- Kafka UI

Local PostgreSQL uses two logical databases:

```text
hotelbooking          -> booking-service
hotelbooking_payment  -> payment-service
```

## 2. Start application services

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

## 3. Check health endpoints

Booking:

```http
GET http://localhost:8080/actuator/health
```

Inventory:

```http
GET http://localhost:8081/actuator/health
```

Notification:

```http
GET http://localhost:8082/actuator/health
```

Payment:

```http
GET http://localhost:8083/actuator/health
```

Audit:

```http
GET http://localhost:8084/actuator/health
```

Expected result:

```json
{
  "status": "UP"
}
```

## 4. Open Swagger UI

Booking:

```http
http://localhost:8080/swagger-ui.html
```

Inventory:

```http
http://localhost:8081/swagger-ui.html
```

Payment:

```http
http://localhost:8083/swagger-ui.html
```

Audit:

```http
http://localhost:8084/swagger-ui.html
```

## 5. Run handmade saga happy path

Use booking-service Swagger:

```http
POST /api/v1/bookings/saga
```

Request body:

```json
{
  "hotelId": "10000000-0000-0000-0000-000000000001",
  "roomTypeId": "20000000-0000-0000-0000-000000000001",
  "checkIn": "2030-06-11",
  "checkOut": "2030-06-12",
  "guestCount": 1,
  "paymentAmount": 1500,
  "paymentCurrency": "RUB"
}
```

`paymentAmount=1500` is below the fake provider decline threshold, so authorization should succeed.

Expected business result:

```text
BookingSaga -> COMPLETED
Booking     -> CONFIRMED
Payment     -> APPROVED
Inventory   -> confirmed reservation
```

Expected booking-service logs:

```text
Booking saga started ... ctx[corr=... saga=... booking=...]
Calling inventory gRPC PlaceHold ... ctx[corr=... saga=... booking=...]
Payment authorized by saga ... ctx[corr=... saga=... booking=...]
Booking saga processing finished ... status=COMPLETED ctx[corr=... saga=... booking=...]
Published booking event to Kafka ... eventType=BookingConfirmed ctx[corr=... booking=... event=... type=BookingConfirmed]
```

Expected inventory-service logs:

```text
Received inventory gRPC PlaceHold ... ctx[corr=... saga=... booking=...]
Inventory hold placed ... ctx[corr=... saga=... booking=...]
Received inventory gRPC ConfirmHold ... ctx[corr=... saga=... booking=...]
```

Expected payment-service logs:

```text
Fake payment provider authorized payment ... ctx[corr=... booking=... payment=...]
Published payment event to Kafka ... ctx[corr=... booking=... payment=... event=... type=PaymentAuthorized]
```

Expected notification-service logs:

```text
Created notification from booking event ... eventType=BookingConfirmed ctx[corr=... booking=... event=... type=BookingConfirmed]
Sending notification through logging adapter ... ctx[corr=... booking=... event=... type=BookingConfirmed]
```

Expected audit-service logs:

```text
Created booking timeline event ... eventType=BookingConfirmed ctx[corr=... booking=... event=... type=BookingConfirmed]
Created payment timeline event ... eventType=PaymentAuthorized ctx[corr=... booking=... payment=... event=... type=PaymentAuthorized]
```

## 6. Run payment-decline scenario

Use booking-service Swagger again:

```http
POST /api/v1/bookings/saga
```

Use the same demo hotel and room type, but set `paymentAmount` above `50000`.

Request body:

```json
{
  "hotelId": "10000000-0000-0000-0000-000000000001",
  "roomTypeId": "20000000-0000-0000-0000-000000000001",
  "checkIn": "2030-06-13",
  "checkOut": "2030-06-14",
  "guestCount": 1,
  "paymentAmount": 50001,
  "paymentCurrency": "RUB"
}
```

Expected business idea:

```text
Inventory hold is attempted first
Payment authorization is declined by the fake provider
Booking must not become CONFIRMED
Saga follows the configured failure/compensation path
```

Expected payment-service logs:

```text
Fake payment provider declined payment ... ctx[corr=... booking=... payment=...]
```

Expected booking-service logs should show that the saga did not complete normally. Depending on the current retry/compensation configuration, the final status may be a failed or compensated saga state, but it should not produce a successful `BookingConfirmed` outcome for this request.

This scenario is useful on an interview because it shows that the demo payment provider is deterministic and that the saga has a negative path, not only a happy path.

## 7. Check booking timeline

Use audit-service:

```http
GET /api/v1/bookings/{bookingId}/timeline
```

Expected event types on a successful flow:

```text
BookingPlacedOnHold
PaymentAuthorized
BookingConfirmed
PaymentApproved
```

The exact ordering can depend on outbox scheduler timing, but all related events should share a common correlation id for the booking creation saga.

For the payment-decline scenario, the timeline should not contain a successful `BookingConfirmed` event for the declined booking.

## 8. Check notification delivery

Notification creation and delivery are separated.

Expected flow for successful booking confirmation:

```text
Kafka BookingConfirmed event
  -> notification document created
  -> scheduler claims pending notification
  -> logging sender sends notification
```

The sender log should still contain the original source context:

```text
ctx[corr=... booking=... event=... type=BookingConfirmed]
```

A scheduler summary log such as `Processed claimed notifications: count=1` may have an empty context. That is expected because it is a batch-level log, not a single notification log.

## 9. Run customer cancellation

Cancel a confirmed booking through booking-service.

Expected result:

```text
Booking -> CANCELLED
Inventory confirmed reservation cancelled
BookingCancelled event published
Notification cancellation message sent
Audit timeline receives BookingCancelled
```

Manual cancellation is a separate business command and may have a different event correlation id from the original booking creation saga.

## 10. Run Spring Statemachine comparison flow

Use booking-service Swagger:

```http
POST /api/v1/bookings/saga-statemachine
```

Use the same happy-path request body:

```json
{
  "hotelId": "10000000-0000-0000-0000-000000000001",
  "roomTypeId": "20000000-0000-0000-0000-000000000001",
  "checkIn": "2030-06-15",
  "checkOut": "2030-06-16",
  "guestCount": 1,
  "paymentAmount": 1500,
  "paymentCurrency": "RUB"
}
```

Expected result is equivalent to the handmade saga for supported scenarios.

The important comparison point:

```text
same shared saga actions
same business outcome
same downstream events
same MDC context style
alternative orchestration mechanism
```

This endpoint exists for learning and comparison. The handmade process manager remains the default production-like implementation.

You can also run the payment-decline request against this endpoint by setting `paymentAmount` to `50001`.

## 11. Check custom metrics

Booking-service:

```http
GET http://localhost:8080/actuator/metrics/hotelbooking.booking.saga.processed
GET http://localhost:8080/actuator/metrics/hotelbooking.booking.outbox.published
```

Payment-service:

```http
GET http://localhost:8083/actuator/metrics/hotelbooking.payment.authorization.processed
GET http://localhost:8083/actuator/metrics/hotelbooking.payment.outbox.published
```

Notification-service:

```http
GET http://localhost:8082/actuator/metrics/hotelbooking.notification.booking_event.processed
GET http://localhost:8082/actuator/metrics/hotelbooking.notification.delivery.processed
```

Expected tags include:

```text
implementation=handmade
implementation=spring-statemachine
outcome=completed
outcome=authorized
outcome=declined
outcome=sent
eventType=BookingConfirmed
```

## 12. Optional: test gRPC deadline behavior

Temporarily set a very small deadline for booking-service:

```text
INVENTORY_GRPC_CLIENT_DEADLINE=PT0.001S
```

Then run a booking saga.

Expected result:

```text
inventory gRPC call fails with deadline-related status
booking saga uses existing retry/failure handling
logs include the deadline value
```

After the test, restore the normal value:

```text
PT3S
```

## 13. Checks before committing

Recommended checks:

```bash
./gradlew :modules:booking:test
./gradlew :modules:payment:test
./gradlew :modules:notification:test
./gradlew :apps:booking-service-app:compileJava
./gradlew :apps:payment-service-app:compileJava
./gradlew :apps:notification-service-app:compileJava
```

Full check:

```bash
./gradlew clean check
```
