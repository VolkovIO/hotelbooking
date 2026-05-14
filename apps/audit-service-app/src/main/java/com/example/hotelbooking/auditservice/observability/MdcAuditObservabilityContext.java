package com.example.hotelbooking.auditservice.observability;

import com.example.hotelbooking.audit.application.event.BookingEventEnvelope;
import com.example.hotelbooking.audit.application.event.PaymentEventEnvelope;
import com.example.hotelbooking.audit.application.port.out.AuditObservabilityContext;
import com.example.hotelbooking.observability.logging.ObservabilityContext;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Audit observability adapter backed by SLF4J MDC.
 *
 * <p>Booking events use aggregateId as bookingId. Payment events use aggregateId as paymentId and
 * contain bookingId inside the payload.
 */
@Component
public class MdcAuditObservabilityContext implements AuditObservabilityContext {

  private static final String BOOKING_ID_PAYLOAD_KEY = "bookingId";

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
  public ContextScope openPaymentEvent(PaymentEventEnvelope event) {
    ObservabilityContext context =
        ObservabilityContext.builder()
            .correlationId(event.correlationId())
            .bookingId(bookingIdFromPayload(event))
            .paymentId(event.aggregateId())
            .eventId(event.eventId())
            .eventType(event.eventType())
            .open();

    return context::close;
  }

  private UUID bookingIdFromPayload(PaymentEventEnvelope event) {
    Object value = event.payload().get(BOOKING_ID_PAYLOAD_KEY);

    if (value == null) {
      return null;
    }

    if (value instanceof UUID uuid) {
      return uuid;
    }

    try {
      return UUID.fromString(value.toString());
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }
}
