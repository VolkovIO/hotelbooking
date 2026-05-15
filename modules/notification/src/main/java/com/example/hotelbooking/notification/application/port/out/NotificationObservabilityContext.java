package com.example.hotelbooking.notification.application.port.out;

import com.example.hotelbooking.notification.application.event.BookingEventEnvelope;
import com.example.hotelbooking.notification.domain.Notification;

/**
 * Port for enriching notification logs with business observability identifiers.
 *
 * <p>The notification module does not depend directly on SLF4J MDC or the shared observability
 * module. It only tells the runtime that a consumed booking event or a persisted notification task
 * should become the active diagnostic context while it is being handled.
 */
public interface NotificationObservabilityContext {

  static NotificationObservabilityContext noop() {
    return new NotificationObservabilityContext() {};
  }

  default ContextScope openBookingEvent(BookingEventEnvelope event) {
    return ContextScope.noop();
  }

  default ContextScope openNotificationDelivery(Notification notification) {
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
