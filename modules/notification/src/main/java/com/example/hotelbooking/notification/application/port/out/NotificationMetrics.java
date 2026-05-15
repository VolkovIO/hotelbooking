package com.example.hotelbooking.notification.application.port.out;

/**
 * Port for recording notification application metrics.
 *
 * <p>The notification module reports low-cardinality business outcomes. The runnable application
 * decides how these metrics are exported.
 */
public interface NotificationMetrics {

  static NotificationMetrics noop() {
    return new NotificationMetrics() {};
  }

  default void bookingEventProcessed(String eventType, String outcome) {}

  default void deliveryProcessed(String channel, String outcome) {}
}
