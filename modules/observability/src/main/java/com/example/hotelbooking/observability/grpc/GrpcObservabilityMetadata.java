package com.example.hotelbooking.observability.grpc;

import com.example.hotelbooking.observability.logging.ObservabilityContext;
import io.grpc.Metadata;
import org.slf4j.MDC;

/**
 * Maps SLF4J MDC observability fields to gRPC metadata headers.
 *
 * <p>gRPC metadata is used here as a lightweight propagation mechanism between services. This is
 * not distributed tracing yet; it only carries business/request identifiers so that logs in booking
 * and inventory can be connected by the same correlation/saga/booking identifiers.
 */
final class GrpcObservabilityMetadata {

  static final Metadata.Key<String> CORRELATION_ID =
      Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);

  static final Metadata.Key<String> SAGA_ID =
      Metadata.Key.of("x-saga-id", Metadata.ASCII_STRING_MARSHALLER);

  static final Metadata.Key<String> BOOKING_ID =
      Metadata.Key.of("x-booking-id", Metadata.ASCII_STRING_MARSHALLER);

  static final Metadata.Key<String> PAYMENT_ID =
      Metadata.Key.of("x-payment-id", Metadata.ASCII_STRING_MARSHALLER);

  static final Metadata.Key<String> EVENT_ID =
      Metadata.Key.of("x-event-id", Metadata.ASCII_STRING_MARSHALLER);

  static final Metadata.Key<String> EVENT_TYPE =
      Metadata.Key.of("x-event-type", Metadata.ASCII_STRING_MARSHALLER);

  private GrpcObservabilityMetadata() {}

  static void copyMdcToHeaders(Metadata headers) {
    putIfPresent(headers, CORRELATION_ID, ObservabilityContext.CORRELATION_ID);
    putIfPresent(headers, SAGA_ID, ObservabilityContext.SAGA_ID);
    putIfPresent(headers, BOOKING_ID, ObservabilityContext.BOOKING_ID);
    putIfPresent(headers, PAYMENT_ID, ObservabilityContext.PAYMENT_ID);
    putIfPresent(headers, EVENT_ID, ObservabilityContext.EVENT_ID);
    putIfPresent(headers, EVENT_TYPE, ObservabilityContext.EVENT_TYPE);
  }

  static ObservabilityContext openContextFromHeaders(Metadata headers) {
    return ObservabilityContext.builder()
        .correlationId(headers.get(CORRELATION_ID))
        .sagaId(headers.get(SAGA_ID))
        .bookingId(headers.get(BOOKING_ID))
        .paymentId(headers.get(PAYMENT_ID))
        .eventId(headers.get(EVENT_ID))
        .eventType(headers.get(EVENT_TYPE))
        .open();
  }

  private static void putIfPresent(Metadata headers, Metadata.Key<String> key, String mdcKey) {
    String value = MDC.get(mdcKey);
    if (value != null && !value.isBlank()) {
      headers.put(key, value);
    }
  }
}
