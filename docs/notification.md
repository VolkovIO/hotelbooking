# Notification Service

## Purpose

Notification Service is responsible for creating and delivering user notifications based on domain events from other services.

In the current version, it consumes booking lifecycle events from Kafka and creates notification records for supported booking events.

The service does not call real external providers yet. EMAIL, TELEGRAM and MAX delivery channels are represented by logging adapters.

## Current scope

Implemented in `v0.8.0`:

- `notification-service` Spring Boot application.
- Notification domain model.
- MongoDB persistence.
- Notification preferences.
- Kafka consumer for booking events.
- Idempotent notification creation.
- Delivery claiming for multi-instance safety.
- Delivery scheduler.
- Logging senders for EMAIL, TELEGRAM and MAX.
- Notification history API.

Real integrations with email, Telegram and MAX are intentionally not implemented yet.

## Module structure

Notification Service follows the same modular style as the rest of the project.

Main areas:

- `domain`
- `application`
- `application ports`
- `inbound adapters`
- `outbound adapters`

The application module is located in:

```text
apps/notification-service-app
```

The notification business module is located in:

```text
modules/notification
```

## Local database ownership

Notification Service owns its own MongoDB database.

Current local database:

```text
hotelbooking_notification
```

Main collections:

- `notifications`
- `notificationPreferences`

The service does not share tables or collections with Booking Service or Inventory Service.

## Kafka consumption

Notification Service consumes events from:

```text
topic: booking.events
consumer group: notification-service
```

Supported booking events:

- `BookingConfirmed`
- `BookingCancelled`

Unsupported booking events are acknowledged and ignored.

Currently ignored booking event example:

- `BookingPlacedOnHold`

This is expected behavior. Booking placement on hold is not considered a user notification in the current version.

## Booking event flow

Typical event flow:

```text
Booking Service
    |
    | booking lifecycle event
    v
Kafka topic: booking.events
    |
    v
Notification Service consumer
    |
    v
MongoDB notification document
```

Booking Service does not call Notification Service directly.

Booking Service only publishes business facts, for example:

- booking was confirmed
- booking was cancelled

Notification Service decides whether and how the user should be notified.

## Idempotency

Notification creation is idempotent.

The `notifications` collection has a unique index on:

- `sourceEventId`
- `type`

This prevents duplicate notification documents when Kafka redelivers an event or when several service instances race to process the same event.

Expected behavior:

```text
same sourceEventId + same notification type
        |
        v
only one notification document
```

## Notification preferences

Notification preferences are stored in the `notificationPreferences` collection.

A preference contains:

- user id
- channel
- destination
- enabled flag
- creation timestamp
- update timestamp

Supported channels:

- `EMAIL`
- `TELEGRAM`
- `MAX`

If a booking event is received and no enabled preference exists for the user, Notification Service creates a `SKIPPED` notification.

This is intentional. It gives an observable audit trail explaining why a notification was not delivered.

## Preference API

Create or update notification preference:

```http
PUT /api/v1/notification-preferences/{userId}
```

Example request:

```json
{
  "channel": "EMAIL",
  "destination": "user@example.com",
  "enabled": true
}
```

Get notification preference:

```http
GET /api/v1/notification-preferences/{userId}
```

Example response:

```json
{
  "userId": "2e1ecd64-e449-49a0-8744-eb5473c8e76b",
  "channel": "EMAIL",
  "destination": "user@example.com",
  "enabled": true,
  "createdAt": "2026-05-07T11:00:00Z",
  "updatedAt": "2026-05-07T11:00:00Z"
}
```

## Notification lifecycle

Notification statuses:

- `PENDING`
- `SENT`
- `FAILED`
- `SKIPPED`

Successful delivery flow:

```text
BookingConfirmed event
        |
        v
Notification PENDING
        |
        v
Delivery scheduler claims notification
        |
        v
Logging sender processes notification
        |
        v
Notification SENT
```

Skipped flow:

```text
BookingConfirmed event
        |
        v
No enabled user preference
        |
        v
Notification SKIPPED
```

## Delivery claiming

Delivery scheduler is designed to be safe with multiple Notification Service instances.

Before sending a notification, a service instance atomically claims it in MongoDB.

Claim fields:

- `lockedBy`
- `lockedUntil`

Only the instance that owns the claim can save the delivery result.

If an instance crashes after claiming a notification, the lock eventually expires and another instance can retry delivery.

This prevents duplicate sending in normal multi-instance operation.

## Logging senders

Current sender adapters do not call external systems.

They only write delivery attempts to application logs.

Implemented logging adapters:

- `LoggingEmailNotificationSender`
- `LoggingTelegramNotificationSender`
- `LoggingMaxNotificationSender`

Example log message:

```text
Sending notification through logging adapter: channel=EMAIL, notificationId=..., userId=..., destination=user@example.com, subject=Booking confirmed, body=Your booking has been confirmed.
```

## Notification history API

Get latest notifications for a user:

```http
GET /api/v1/notifications?userId={userId}&limit=10
```

Rules:

- default limit: `10`
- maximum limit: `100`
- sorting: newest first

Example:

```http
GET /api/v1/notifications?userId=2e1ecd64-e449-49a0-8744-eb5473c8e76b&limit=10
```

## Local run

Start infrastructure:

```bash
docker compose up -d
```

Run Notification Service:

```bash
gradlew.bat :apps:notification-service-app:bootRun --args="--spring.profiles.active=local"
```

Expected local profile group:

- `notification-mongo`
- `notification-kafka`
- `notification-senders-logging`

## Local verification scenario

1. Start Kafka, MongoDB, Booking Service, Inventory Service and Notification Service.
2. Create notification preference for a user.
3. Create a booking.
4. Confirm the booking.
5. Check Notification Service logs.
6. Check the `notifications` collection in MongoDB.
7. Call notification history API.

Expected behavior:

- `BookingPlacedOnHold` is ignored.
- `BookingConfirmed` creates a notification.
- Delivery scheduler claims the notification.
- Logging EMAIL sender writes a log message.
- Notification status becomes `SENT`.

## Example Mongo document

```json
{
  "_id": "aef67c5e-29ad-42d4-bd82-202236226a85",
  "sourceEventId": "055ed675-9194-4b71-a6c8-97aa5f286d2a",
  "sourceEventType": "BookingConfirmed",
  "type": "BOOKING_CONFIRMED",
  "userId": "2e1ecd64-e449-49a0-8744-eb5473c8e76b",
  "channel": "EMAIL",
  "destination": "user@example.com",
  "subject": "Booking confirmed",
  "body": "Your booking has been confirmed.",
  "status": "SENT",
  "attempts": 0,
  "createdAt": "2026-05-07T19:53:27.881Z",
  "sentAt": "2026-05-07T19:53:32.767Z",
  "updatedAt": "2026-05-07T19:53:32.767Z"
}
```

## Current limitations

- No real email provider.
- No real Telegram integration.
- No real MAX integration.
- No authentication on notification APIs.
- No authorization checks on preference and history APIs.
- No user/profile service integration.
- Only one notification preference per user.
- Notification text is static.
- No localization.
- No templates.
- No provider-specific retry classification.
- No notification DLQ.
