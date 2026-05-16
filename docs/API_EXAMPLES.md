# API Examples

These examples are intended for local demo runs.

For Google-authenticated booking calls, set:

```bash
TOKEN=<google-id-token>
```

In demo auth mode, omit the `Authorization` header and run `booking-service` with `dev`.

## Inventory catalog

Find hotels:

```bash
curl "http://localhost:8081/api/v1/hotels?limit=20"
```

Get hotel by id:

```bash
curl "http://localhost:8081/api/v1/hotels/$HOTEL_ID"
```

Get availability:

```bash
curl "http://localhost:8081/api/v1/hotels/$HOTEL_ID/room-types/$ROOM_TYPE_ID/availability?from=2030-06-11&to=2030-06-20"
```

## Inventory admin

Create hotel:

```bash
curl -X POST "http://localhost:8081/api/v1/admin/hotels" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Portfolio Demo Hotel",
    "city": "Kazan"
  }'
```

Add room type:

```bash
curl -X POST "http://localhost:8081/api/v1/admin/hotels/$HOTEL_ID/room-types" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Standard",
    "guestCapacity": 2
  }'
```

Initialize availability:

```bash
curl -X POST "http://localhost:8081/api/v1/admin/hotels/$HOTEL_ID/room-types/$ROOM_TYPE_ID/availability/initialization" \
  -H "Content-Type: application/json" \
  -d '{
    "from": "2030-06-11",
    "to": "2030-06-20",
    "totalRooms": 10
  }'
```

## Booking saga

Start saga:

```bash
curl -X POST "http://localhost:8080/api/v1/bookings/saga" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "hotelId": "'"$HOTEL_ID"'",
    "roomTypeId": "'"$ROOM_TYPE_ID"'",
    "checkIn": "2030-06-11",
    "checkOut": "2030-06-12",
    "guestCount": 2,
    "paymentAmount": 1500,
    "paymentCurrency": "RUB"
  }'
```

Start compensation scenario:

```bash
curl -X POST "http://localhost:8080/api/v1/bookings/saga" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "hotelId": "'"$HOTEL_ID"'",
    "roomTypeId": "'"$ROOM_TYPE_ID"'",
    "checkIn": "2030-06-11",
    "checkOut": "2030-06-12",
    "guestCount": 2,
    "paymentAmount": 150000,
    "paymentCurrency": "RUB"
  }'
```

Get current user bookings:

```bash
curl "http://localhost:8080/api/v1/bookings/my?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

Cancel booking:

```bash
curl -X POST "http://localhost:8080/api/v1/bookings/$BOOKING_ID/cancel" \
  -H "Authorization: Bearer $TOKEN"
```

## Audit timeline

```bash
curl "http://localhost:8084/api/v1/bookings/$BOOKING_ID/timeline"
```

## Health checks

```bash
curl "http://localhost:8080/actuator/health"
curl "http://localhost:8081/actuator/health"
curl "http://localhost:8082/actuator/health"
curl "http://localhost:8083/actuator/health"
curl "http://localhost:8084/actuator/health"
```

