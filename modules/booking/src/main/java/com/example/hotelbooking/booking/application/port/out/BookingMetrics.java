package com.example.hotelbooking.booking.application.port.out;

/**
 * Port for recording booking application metrics.
 *
 * <p>The booking module does not depend directly on Micrometer. It only reports business-level
 * events such as saga outcomes and outbox publication results. The runnable application decides how
 * these signals are exported.
 */
public interface BookingMetrics {

  static BookingMetrics noop() {
    return new BookingMetrics() {};
  }

  default void sagaProcessed(String implementation, String outcome) {}

  default void sagaRetryScheduled(String implementation) {}

  default void outboxMessagePublished(String eventType) {}

  default void outboxMessagePublicationFailed(String eventType, String failure) {}
}
