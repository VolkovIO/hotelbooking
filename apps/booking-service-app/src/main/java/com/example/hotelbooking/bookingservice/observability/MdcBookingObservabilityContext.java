package com.example.hotelbooking.bookingservice.observability;

import com.example.hotelbooking.booking.application.event.BookingOutboxMessage;
import com.example.hotelbooking.booking.application.port.out.BookingObservabilityContext;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaId;
import com.example.hotelbooking.booking.domain.BookingId;
import com.example.hotelbooking.observability.logging.ObservabilityContext;
import org.springframework.stereotype.Component;

/**
 * Booking observability adapter backed by SLF4J MDC.
 *
 * <p>This class belongs to the booking-service application layer, not to the booking module. The
 * booking module depends only on the BookingObservabilityContext port and therefore does not know
 * how diagnostic data is written to logs.
 */
@Component
public class MdcBookingObservabilityContext implements BookingObservabilityContext {

  @Override
  public ContextScope openSaga(BookingSaga saga) {
    ObservabilityContext context =
        ObservabilityContext.builder()
            .sagaId(saga.getId().value())
            .bookingId(saga.getBookingId())
            .paymentId(saga.getPaymentId())
            .open();

    return context::close;
  }

  @Override
  public ContextScope openSaga(BookingSagaId sagaId) {
    ObservabilityContext context = ObservabilityContext.builder().sagaId(sagaId.value()).open();

    return context::close;
  }

  @Override
  public ContextScope openBooking(BookingId bookingId) {
    ObservabilityContext context =
        ObservabilityContext.builder().bookingId(bookingId.value()).open();

    return context::close;
  }

  @Override
  public ContextScope openOutboxMessage(BookingOutboxMessage message) {
    ObservabilityContext context =
        ObservabilityContext.builder()
            .correlationId(message.correlationId())
            .bookingId(message.aggregateId())
            .eventId(message.id())
            .eventType(message.eventType())
            .open();

    return context::close;
  }
}
