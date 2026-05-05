package com.example.hotelbooking.booking.application.event;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record BookingOutboxMessage(
    UUID id,
    String aggregateType,
    UUID aggregateId,
    String eventType,
    int eventVersion,
    Map<String, Object> payload,
    Instant occurredAt,
    int attempts) {

  private static final String BOOKING_AGGREGATE_TYPE = "Booking";

  public BookingOutboxMessage {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(aggregateType, "aggregateType must not be null");
    Objects.requireNonNull(aggregateId, "aggregateId must not be null");
    Objects.requireNonNull(eventType, "eventType must not be null");
    Objects.requireNonNull(payload, "payload must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    payload = Map.copyOf(payload);
  }

  public static BookingOutboxMessage from(BookingLifecycleEvent event) {
    return new BookingOutboxMessage(
        event.eventId(),
        BOOKING_AGGREGATE_TYPE,
        event.bookingId().value(),
        event.eventType(),
        event.eventVersion(),
        event.payload(),
        event.occurredAt(),
        0);
  }
}
