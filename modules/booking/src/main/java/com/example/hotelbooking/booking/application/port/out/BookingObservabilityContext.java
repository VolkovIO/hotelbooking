package com.example.hotelbooking.booking.application.port.out;

import com.example.hotelbooking.booking.application.event.BookingOutboxMessage;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaId;
import com.example.hotelbooking.booking.domain.BookingId;

/**
 * Port for enriching logs and diagnostic context around booking application operations.
 *
 * <p>The booking module intentionally exposes this as a port instead of depending directly on SLF4J
 * MDC or on a shared observability module. This keeps the direction of dependencies clean: booking
 * use cases describe which business identifiers are useful for diagnostics, while the application
 * runtime decides how to put those identifiers into logs, traces or metrics.
 *
 * <p>The default implementation is a no-op. Unit tests and non-observable runtimes can safely use
 * {@link #noop()}.
 */
public interface BookingObservabilityContext {

  static BookingObservabilityContext noop() {
    return new BookingObservabilityContext() {};
  }

  default ContextScope openSaga(BookingSaga saga) {
    return ContextScope.noop();
  }

  default ContextScope openSaga(BookingSagaId sagaId) {
    return ContextScope.noop();
  }

  default ContextScope openBooking(BookingId bookingId) {
    return ContextScope.noop();
  }

  default ContextScope openOutboxMessage(BookingOutboxMessage message) {
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
