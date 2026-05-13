package com.example.hotelbooking.observability.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * Thread-local logging context helper based on SLF4J MDC.
 *
 * <p>The helper stores previous MDC values and restores them when the scope is closed. This makes
 * nested scopes safe. For example, an HTTP request can set {@code correlationId}, then an outbox
 * publisher can temporarily set another event-specific {@code correlationId}; when the outbox scope
 * closes, the HTTP request value is restored instead of being lost.
 *
 * <p>Always use it with try-with-resources.
 */
public final class ObservabilityContext implements AutoCloseable {

  public static final String CORRELATION_ID = "correlationId";
  public static final String SAGA_ID = "sagaId";
  public static final String BOOKING_ID = "bookingId";
  public static final String PAYMENT_ID = "paymentId";
  public static final String EVENT_ID = "eventId";
  public static final String EVENT_TYPE = "eventType";

  private final List<PreviousMdcValue> previousValues;

  private ObservabilityContext(List<PreviousMdcValue> previousValues) {
    this.previousValues = List.copyOf(previousValues);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void close() {
    for (int index = previousValues.size() - 1; index >= 0; index--) {
      previousValues.get(index).restore();
    }
  }

  private record PreviousMdcValue(String key, String previousValue) {

    private void restore() {
      if (previousValue == null) {
        MDC.remove(key);
      } else {
        MDC.put(key, previousValue);
      }
    }
  }

  public static final class Builder {

    private final List<PreviousMdcValue> previousValues = new ArrayList<>();

    private Builder() {}

    public Builder correlationId(UUID value) {
      return putUuid(CORRELATION_ID, value);
    }

    public Builder sagaId(UUID value) {
      return putUuid(SAGA_ID, value);
    }

    public Builder bookingId(UUID value) {
      return putUuid(BOOKING_ID, value);
    }

    public Builder paymentId(UUID value) {
      return putUuid(PAYMENT_ID, value);
    }

    public Builder eventId(UUID value) {
      return putUuid(EVENT_ID, value);
    }

    public Builder eventType(String value) {
      return put(EVENT_TYPE, value);
    }

    public ObservabilityContext open() {
      return new ObservabilityContext(previousValues);
    }

    private Builder putUuid(String key, UUID value) {
      if (value != null) {
        put(key, value.toString());
      }
      return this;
    }

    private Builder put(String key, String value) {
      if (value != null && !value.isBlank()) {
        previousValues.add(new PreviousMdcValue(key, MDC.get(key)));
        MDC.put(key, value);
      }
      return this;
    }
  }
}
