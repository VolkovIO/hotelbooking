package com.example.hotelbooking.payment.application.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record PaymentEventEnvelope(
    UUID eventId,
    String eventType,
    int eventVersion,
    String aggregateType,
    UUID aggregateId,
    Instant occurredAt,
    UUID correlationId,
    UUID causationId,
    Map<String, Object> payload) {

  public PaymentEventEnvelope {
    Objects.requireNonNull(eventId, "eventId must not be null");
    Objects.requireNonNull(eventType, "eventType must not be null");
    Objects.requireNonNull(aggregateType, "aggregateType must not be null");
    Objects.requireNonNull(aggregateId, "aggregateId must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    Objects.requireNonNull(correlationId, "correlationId must not be null");
    Objects.requireNonNull(payload, "payload must not be null");

    payload = Map.copyOf(payload);
  }

  public static PaymentEventEnvelope from(PaymentOutboxMessage message) {
    return new PaymentEventEnvelope(
        message.eventId(),
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
