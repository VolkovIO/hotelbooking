package com.example.hotelbooking.notificationservice.observability;

import com.example.hotelbooking.notification.application.event.BookingEventEnvelope;
import com.example.hotelbooking.notification.application.port.out.NotificationObservabilityContext;
import com.example.hotelbooking.notification.domain.Notification;
import com.example.hotelbooking.observability.logging.ObservabilityContext;
import org.springframework.stereotype.Component;

/**
 * Notification observability adapter backed by SLF4J MDC.
 *
 * <p>Booking events use aggregateId as bookingId, so notification logs can be connected back to the
 * original booking flow. Persisted notification tasks keep the same source identifiers and restore
 * them during scheduled delivery.
 */
@Component
public class MdcNotificationObservabilityContext implements NotificationObservabilityContext {

  @Override
  public ContextScope openBookingEvent(BookingEventEnvelope event) {
    ObservabilityContext context =
        ObservabilityContext.builder()
            .correlationId(event.correlationId())
            .bookingId(event.aggregateId())
            .eventId(event.eventId())
            .eventType(event.eventType())
            .open();

    return context::close;
  }

  @Override
  public ContextScope openNotificationDelivery(Notification notification) {
    ObservabilityContext context =
        ObservabilityContext.builder()
            .correlationId(notification.getSourceCorrelationId().value())
            .bookingId(notification.getSourceAggregateId().value())
            .eventId(notification.getSourceEventId().value())
            .eventType(notification.getSourceEventType().value())
            .open();

    return context::close;
  }
}
