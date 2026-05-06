package com.example.hotelbooking.booking.application.event;

import java.time.Instant;
import java.util.Map;
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
