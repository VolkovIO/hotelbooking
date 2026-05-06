package com.example.hotelbooking.booking.application.event;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record BookingEventEnvelope(
    UUID eventId,
    String eventType,
    int eventVersion,
    String aggregateType,
    UUID aggregateId,
    Instant occurredAt,
    UUID correlationId,
    UUID causationId,
    Map<String, Object> payload) {

  public BookingEventEnvelope {
    Objects.requireNonNull(eventId, "eventId must not be null");
    Objects.requireNonNull(eventType, "eventType must not be null");
    Objects.requireNonNull(aggregateType, "aggregateType must not be null");
    Objects.requireNonNull(aggregateId, "aggregateId must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    Objects.requireNonNull(correlationId, "correlationId must not be null");

    payload = Map.copyOf(Objects.requireNonNull(payload, "payload must not be null"));
  }

  public static BookingEventEnvelope from(BookingOutboxMessage message) {
    return new BookingEventEnvelope(
        message.id(),
        message.eventType(),
        message.eventVersion(),
        message.aggregateType(),
        message.aggregateId(),
        message.occurredAt(),
        message.correlationId(),
        message.causationId(),
        message.payload());
  }
}
