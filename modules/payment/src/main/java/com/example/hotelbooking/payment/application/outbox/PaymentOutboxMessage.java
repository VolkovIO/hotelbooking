package com.example.hotelbooking.payment.application.outbox;

import com.example.hotelbooking.payment.domain.event.PaymentLifecycleEvent;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record PaymentOutboxMessage(
    UUID eventId,
    String eventType,
    int eventVersion,
    String aggregateType,
    UUID aggregateId,
    Map<String, Object> payload,
    Instant occurredAt,
    UUID correlationId,
    UUID causationId,
    PaymentOutboxStatus processingStatus,
    int retryCount,
    Instant nextAttemptAt,
    String lastError,
    Instant createdAt,
    Instant publishedAt,
    Instant updatedAt) {

  public PaymentOutboxMessage {
    Objects.requireNonNull(eventId, "eventId must not be null");
    Objects.requireNonNull(eventType, "eventType must not be null");
    Objects.requireNonNull(aggregateType, "aggregateType must not be null");
    Objects.requireNonNull(aggregateId, "aggregateId must not be null");
    Objects.requireNonNull(payload, "payload must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    Objects.requireNonNull(correlationId, "correlationId must not be null");
    Objects.requireNonNull(processingStatus, "processingStatus must not be null");
    Objects.requireNonNull(nextAttemptAt, "nextAttemptAt must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");

    if (retryCount < 0) {
      throw new IllegalArgumentException("retryCount must not be negative");
    }

    payload = Map.copyOf(payload);
  }

  public static PaymentOutboxMessage from(PaymentLifecycleEvent event) {
    Instant now = Instant.now();

    return new PaymentOutboxMessage(
        event.eventId(),
        event.eventType(),
        event.eventVersion(),
        event.aggregateType(),
        event.aggregateId(),
        event.payload(),
        event.occurredAt(),
        event.correlationId(),
        event.causationId(),
        PaymentOutboxStatus.NEW,
        0,
        now,
        null,
        now,
        null,
        now);
  }
}
