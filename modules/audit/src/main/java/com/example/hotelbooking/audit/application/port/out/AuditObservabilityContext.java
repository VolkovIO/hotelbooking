package com.example.hotelbooking.audit.application.port.out;

import com.example.hotelbooking.audit.application.event.BookingEventEnvelope;
import com.example.hotelbooking.audit.application.event.PaymentEventEnvelope;

/**
 * Port for enriching audit logs with business observability identifiers.
 *
 * <p>The audit module exposes this as a port instead of depending directly on SLF4J MDC or on the
 * shared observability module. Kafka adapters describe which event identifiers are useful for
 * diagnostics, while the runnable application decides how to write them into logs.
 */
public interface AuditObservabilityContext {

  static AuditObservabilityContext noop() {
    return new AuditObservabilityContext() {};
  }

  default ContextScope openBookingEvent(BookingEventEnvelope event) {
    return ContextScope.noop();
  }

  default ContextScope openPaymentEvent(PaymentEventEnvelope event) {
    return ContextScope.noop();
  }

  @FunctionalInterface
  interface ContextScope extends AutoCloseable {

    static ContextScope noop() {
      return () -> {};
    }

    @Override
    void close();
  }
}
