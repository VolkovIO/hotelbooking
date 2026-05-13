package com.example.hotelbooking.audit.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record TimelineEvent(
    UUID eventId,
    String eventType,
    int eventVersion,
    String source,
    String aggregateType,
    UUID aggregateId,
    UUID bookingId,
    Instant occurredAt,
    UUID correlationId,
    UUID causationId,
    Map<String, Object> payload,
    Instant recordedAt) {

  public TimelineEvent {
    Objects.requireNonNull(eventId, "eventId must not be null");
    Objects.requireNonNull(eventType, "eventType must not be null");
    Objects.requireNonNull(source, "source must not be null");
    Objects.requireNonNull(aggregateType, "aggregateType must not be null");
    Objects.requireNonNull(aggregateId, "aggregateId must not be null");
    Objects.requireNonNull(bookingId, "bookingId must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    Objects.requireNonNull(recordedAt, "recordedAt must not be null");

    payload = Map.copyOf(Objects.requireNonNull(payload, "payload must not be null"));
  }
}
