package com.example.hotelbooking.payment.application.port.out;

import com.example.hotelbooking.payment.application.outbox.PaymentOutboxMessage;
import com.example.hotelbooking.payment.domain.BookingId;
import com.example.hotelbooking.payment.domain.Payment;
import com.example.hotelbooking.payment.domain.PaymentId;
import java.util.UUID;

/**
 * Port for enriching logs and diagnostic context around payment application operations.
 *
 * <p>The payment module intentionally exposes this as a port instead of depending directly on SLF4J
 * MDC or on a shared observability module. This keeps the payment application layer independent
 * from logging infrastructure while still allowing the runtime application to attach useful
 * business identifiers to logs.
 */
public interface PaymentObservabilityContext {

  static PaymentObservabilityContext noop() {
    return new PaymentObservabilityContext() {};
  }

  default ContextScope openBooking(UUID correlationId, BookingId bookingId) {
    return ContextScope.noop();
  }

  default ContextScope openPayment(UUID correlationId, Payment payment) {
    return ContextScope.noop();
  }

  default ContextScope openPayment(UUID correlationId, PaymentId paymentId) {
    return ContextScope.noop();
  }

  default ContextScope openOutboxMessage(PaymentOutboxMessage message) {
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
