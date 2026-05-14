package com.example.hotelbooking.notificationservice.observability;

import com.example.hotelbooking.notification.application.port.out.NotificationMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Micrometer-backed implementation of notification application metrics.
 *
 * <p>Only low-cardinality tags are used: event type, channel and outcome.
 */
@Component
@RequiredArgsConstructor
public class MicrometerNotificationMetrics implements NotificationMetrics {

  private static final String TAG_EVENT_TYPE = "eventType";
  private static final String TAG_CHANNEL = "channel";
  private static final String TAG_OUTCOME = "outcome";

  private final MeterRegistry meterRegistry;

  @Override
  public void bookingEventProcessed(String eventType, String outcome) {
    Counter.builder("hotelbooking.notification.booking_event.processed")
        .description("Number of consumed booking events by notification-service outcome")
        .tag(TAG_EVENT_TYPE, eventType)
        .tag(TAG_OUTCOME, outcome)
        .register(meterRegistry)
        .increment();
  }

  @Override
  public void deliveryProcessed(String channel, String outcome) {
    Counter.builder("hotelbooking.notification.delivery.processed")
        .description("Number of notification delivery attempts by channel and outcome")
        .tag(TAG_CHANNEL, channel)
        .tag(TAG_OUTCOME, outcome)
        .register(meterRegistry)
        .increment();
  }
}
