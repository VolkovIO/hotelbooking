package com.example.hotelbooking.bookingservice.observability;

import com.example.hotelbooking.booking.application.port.out.BookingMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Micrometer-backed implementation of booking application metrics.
 *
 * <p>These metrics are intentionally small and business-oriented. They are useful for a local demo
 * and for explaining how the system can be observed without introducing a full monitoring stack
 * yet.
 */
@Component
@RequiredArgsConstructor
public class MicrometerBookingMetrics implements BookingMetrics {

  private static final String TAG_IMPLEMENTATION = "implementation";
  private static final String TAG_OUTCOME = "outcome";
  private static final String TAG_EVENT_TYPE = "eventType";
  private static final String TAG_FAILURE = "failure";

  private final MeterRegistry meterRegistry;

  @Override
  public void sagaProcessed(String implementation, String outcome) {
    Counter.builder("hotelbooking.booking.saga.processed")
        .description("Number of processed booking saga runs by implementation and outcome")
        .tag(TAG_IMPLEMENTATION, implementation)
        .tag(TAG_OUTCOME, outcome)
        .register(meterRegistry)
        .increment();
  }

  @Override
  public void sagaRetryScheduled(String implementation) {
    Counter.builder("hotelbooking.booking.saga.retry.scheduled")
        .description("Number of booking saga retries scheduled")
        .tag(TAG_IMPLEMENTATION, implementation)
        .register(meterRegistry)
        .increment();
  }

  @Override
  public void outboxMessagePublished(String eventType) {
    Counter.builder("hotelbooking.booking.outbox.published")
        .description("Number of booking outbox messages published successfully")
        .tag(TAG_EVENT_TYPE, eventType)
        .register(meterRegistry)
        .increment();
  }

  @Override
  public void outboxMessagePublicationFailed(String eventType, String failure) {
    Counter.builder("hotelbooking.booking.outbox.publication.failed")
        .description("Number of booking outbox publication failures")
        .tag(TAG_EVENT_TYPE, eventType)
        .tag(TAG_FAILURE, failure)
        .register(meterRegistry)
        .increment();
  }
}
