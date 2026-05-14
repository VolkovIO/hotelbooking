package com.example.hotelbooking.notificationservice.observability;

import com.example.hotelbooking.notification.application.event.BookingEventEnvelope;
import com.example.hotelbooking.notification.application.port.out.NotificationObservabilityContext;
import com.example.hotelbooking.observability.logging.ObservabilityContext;
import org.springframework.stereotype.Component;

/**
 * Notification observability adapter backed by SLF4J MDC.
 *
 * <p>Booking events use aggregateId as bookingId, so notification logs can be connected back to the
 * original booking flow.
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
}
