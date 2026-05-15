package com.example.hotelbooking.paymentservice.observability;

import com.example.hotelbooking.observability.logging.ObservabilityContext;
import com.example.hotelbooking.payment.application.outbox.PaymentOutboxMessage;
import com.example.hotelbooking.payment.application.port.out.PaymentObservabilityContext;
import com.example.hotelbooking.payment.domain.BookingId;
import com.example.hotelbooking.payment.domain.Payment;
import com.example.hotelbooking.payment.domain.PaymentId;
import org.springframework.stereotype.Component;

/**
 * Payment observability adapter backed by SLF4J MDC.
 *
 * <p>The payment module depends only on PaymentObservabilityContext. This adapter belongs to the
 * payment-service application and translates payment business identifiers to the shared MDC keys
 * used by the logging pattern.
 */
@Component
public class MdcPaymentObservabilityContext implements PaymentObservabilityContext {

  @Override
  public ContextScope openBooking(java.util.UUID correlationId, BookingId bookingId) {
    ObservabilityContext context =
        ObservabilityContext.builder()
            .correlationId(correlationId)
            .bookingId(bookingId.value())
            .open();

    return context::close;
  }

  @Override
  public ContextScope openPayment(java.util.UUID correlationId, Payment payment) {
    ObservabilityContext context =
        ObservabilityContext.builder()
            .correlationId(correlationId)
            .bookingId(payment.getBookingId().value())
            .paymentId(payment.getId().value())
            .open();

    return context::close;
  }

  @Override
  public ContextScope openPayment(java.util.UUID correlationId, PaymentId paymentId) {
    ObservabilityContext context =
        ObservabilityContext.builder()
            .correlationId(correlationId)
            .paymentId(paymentId.value())
            .open();

    return context::close;
  }

  @Override
  public ContextScope openOutboxMessage(PaymentOutboxMessage message) {
    ObservabilityContext context =
        ObservabilityContext.builder()
            .correlationId(message.correlationId())
            .paymentId(message.aggregateId())
            .eventId(message.eventId())
            .eventType(message.eventType())
            .open();

    return context::close;
  }
}
