# Logging Strategy

## Purpose

Application logs are used to understand business flows, integration boundaries and failures.

The project currently has two separately runnable services:

```text
booking-service-app
inventory-service-app
```

Booking communicates with inventory through gRPC.

Logging should help answer:

```text
What business operation was requested?
Which service handled it?
Which integration boundary was called?
Did the operation succeed or fail?
Which bookingId, holdId, hotelId or roomTypeId was involved?
```

## Logging levels

### INFO

Use `INFO` for important business lifecycle events and startup-level application configuration.

Examples:

```text
Booking created
Booking confirmed
Booking cancelled
Inventory hold placed
Inventory hold confirmed
Inventory hold released
Inventory gRPC server started
Inventory gRPC client channel created
```

`INFO` logs should be useful in normal local and production-like runs.

### DEBUG

Use `DEBUG` for technical boundary details.

Examples:

```text
Calling inventory gRPC FindRoomTypeReference
Calling inventory gRPC PlaceHold
Received inventory gRPC PlaceHold
Inventory gRPC PlaceHold completed
Booking found by id
Room type reference found
```

`DEBUG` logs are useful during local development and troubleshooting.

They should not be enabled by default in production-like environments because they may be noisy.

### WARN

Use `WARN` for expected but problematic situations.

Examples:

```text
Inventory gRPC call failed
Inventory domain error mapped to gRPC status
Inventory application error mapped to gRPC status
Business operation rejected
Downstream service unavailable
```

`WARN` should include enough context to diagnose the issue, but should not dump full sensitive payloads.

### ERROR

Use `ERROR` for unexpected technical failures.

Most unexpected errors are currently handled by Spring Boot and global exception handling.

Avoid logging the same exception multiple times at different layers.

### TRACE

`TRACE` is not used by default.

It may be introduced later for very detailed diagnostics, but should remain disabled in normal runs.

## Where to log

### Booking application services

Booking lifecycle operations are logged at the application service boundary.

Examples:

```text
CreateBookingService
ConfirmBookingService
CancelBookingService
GetBookingByIdService
```

This provides visibility into booking business operations without polluting the domain model.

### Inventory application services

Inventory lifecycle operations are logged at the application service boundary.

Examples:

```text
RegisterHotelService
AddRoomTypeService
InitializeRoomAvailabilityService
AdjustRoomCapacityService
InventoryReservationService
```

This provides visibility into administrative inventory changes and reservation state changes.

### gRPC client adapters

Booking gRPC client adapters log outbound calls to inventory.

Examples:

```text
GrpcInventoryLookupAdapter
GrpcInventoryReservationAdapter
BookingInventoryGrpcExceptionMapper
```

Regular request/response details are logged at `DEBUG`.

Failed gRPC calls are logged at `WARN`.

### gRPC server adapters

Inventory gRPC server adapters log inbound calls from booking.

Examples:

```text
InventoryQueryGrpcService
InventoryReservationGrpcService
InventoryGrpcExceptionMapper
```

Regular request/response details are logged at `DEBUG`.

Mapped domain/application errors are logged at `WARN`.

## Where not to log

Do not log inside domain objects.

Examples:

```text
Booking
Hotel
RoomAvailability
RoomHold
RoomType
```

The domain model should remain free of logging and infrastructure concerns.

Do not log every SQL or MongoDB operation manually. Database-level diagnostics should be handled through framework logging or observability tools.

## Sensitive data

Do not log:

```text
access tokens
refresh tokens
passwords
authorization headers
payment data
full personal data
```

When user identity is introduced, prefer stable internal identifiers:

```text
userId
bookingId
```

Avoid logging raw OAuth2/JWT tokens or full external identity payloads.

## Local debug configuration

### Enable all project DEBUG logs

```text
--logging.level.com.example.hotelbooking=DEBUG
```

### Enable booking gRPC client DEBUG logs

```text
--logging.level.com.example.hotelbooking.booking.adapter.out.integration.inventory.grpc=DEBUG
```

### Enable inventory gRPC server DEBUG logs

```text
--logging.level.com.example.hotelbooking.inventory.adapter.in.grpc=DEBUG
```

## IntelliJ IDEA examples

Booking service Program arguments:

```text
--spring.profiles.active=booking-postgres,inventory-grpc-client --logging.level.com.example.hotelbooking.booking.adapter.out.integration.inventory.grpc=DEBUG
```

Inventory service Program arguments:

```text
--spring.profiles.active=inventory-mongo,inventory-grpc-server --logging.level.com.example.hotelbooking.inventory.adapter.in.grpc=DEBUG
```

## Future observability direction

This logging strategy prepares the project for future observability work:

```text
structured JSON logs
ELK / OpenSearch
Prometheus + Grafana
OpenTelemetry tracing
correlation IDs
trace IDs
gRPC metadata propagation
```

The next observability steps should focus on:

```text
1. correlation IDs across HTTP and gRPC
2. structured logs
3. metrics with Micrometer and Prometheus
4. distributed tracing with OpenTelemetry
5. log aggregation with ELK or OpenSearch
```
