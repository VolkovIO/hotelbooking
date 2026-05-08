package com.example.hotelbooking.payment.domain.event;

import com.example.hotelbooking.payment.domain.Payment;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record PaymentLifecycleEvent(
    UUID eventId,
    String eventType,
    int eventVersion,
    String aggregateType,
    UUID aggregateId,
    Map<String, Object> payload,
    Instant occurredAt,
    UUID correlationId,
    UUID causationId) {

  private static final int EVENT_VERSION = 1;
  private static final String AGGREGATE_TYPE = "Payment";

  public PaymentLifecycleEvent {
    Objects.requireNonNull(eventId, "eventId must not be null");
    Objects.requireNonNull(eventType, "eventType must not be null");
    Objects.requireNonNull(aggregateType, "aggregateType must not be null");
    Objects.requireNonNull(aggregateId, "aggregateId must not be null");
    Objects.requireNonNull(payload, "payload must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    Objects.requireNonNull(correlationId, "correlationId must not be null");

    payload = Map.copyOf(payload);
  }

  public static PaymentLifecycleEvent authorized(Payment payment) {
    return create("PaymentAuthorized", payment);
  }

  public static PaymentLifecycleEvent declined(Payment payment) {
    return create("PaymentDeclined", payment);
  }

  public static PaymentLifecycleEvent approved(Payment payment) {
    return create("PaymentApproved", payment);
  }

  public static PaymentLifecycleEvent cancelled(Payment payment) {
    return create("PaymentCancelled", payment);
  }

  private static PaymentLifecycleEvent create(String eventType, Payment payment) {
    UUID eventId = UUID.randomUUID();

    return new PaymentLifecycleEvent(
        eventId,
        eventType,
        EVENT_VERSION,
        AGGREGATE_TYPE,
        payment.getId().value(),
        payload(payment),
        Instant.now(),
        eventId,
        null);
  }

  private static Map<String, Object> payload(Payment payment) {
    Map<String, Object> payload = new LinkedHashMap<>();

    payload.put("paymentId", payment.getId().value());
    payload.put("bookingId", payment.getBookingId().value());
    payload.put("userId", payment.getUserId().value());
    payload.put("amount", payment.getAmount().value());
    payload.put("currency", payment.getCurrency().value());
    payload.put("provider", payment.getProvider().name());
    payload.put("status", payment.getStatus().name());

    if (payment.getProviderPaymentId() != null) {
      payload.put("providerPaymentId", payment.getProviderPaymentId().value());
    }

    if (payment.getFailureReason() != null) {
      payload.put("failureReason", payment.getFailureReason().value());
    }

    return payload;
  }
}
