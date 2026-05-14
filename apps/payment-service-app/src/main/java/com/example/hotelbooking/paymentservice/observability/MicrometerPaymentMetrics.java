package com.example.hotelbooking.paymentservice.observability;

import com.example.hotelbooking.payment.application.port.out.PaymentMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Micrometer-backed implementation of payment application metrics.
 *
 * <p>The metrics are intentionally business-oriented and low-cardinality. They are safe to expose
 * through Actuator metrics in local/demo environments.
 */
@Component
@RequiredArgsConstructor
public class MicrometerPaymentMetrics implements PaymentMetrics {

  private static final String TAG_OUTCOME = "outcome";
  private static final String TAG_EVENT_TYPE = "eventType";
  private static final String TAG_FAILURE = "failure";

  private final MeterRegistry meterRegistry;

  @Override
  public void authorizationProcessed(String outcome) {
    Counter.builder("hotelbooking.payment.authorization.processed")
        .description("Number of payment authorization attempts by outcome")
        .tag(TAG_OUTCOME, outcome)
        .register(meterRegistry)
        .increment();
  }

  @Override
  public void approvalProcessed(String outcome) {
    Counter.builder("hotelbooking.payment.approval.processed")
        .description("Number of payment approval attempts by outcome")
        .tag(TAG_OUTCOME, outcome)
        .register(meterRegistry)
        .increment();
  }

  @Override
  public void cancellationProcessed(String outcome) {
    Counter.builder("hotelbooking.payment.cancellation.processed")
        .description("Number of payment cancellation attempts by outcome")
        .tag(TAG_OUTCOME, outcome)
        .register(meterRegistry)
        .increment();
  }

  @Override
  public void outboxMessagePublished(String eventType) {
    Counter.builder("hotelbooking.payment.outbox.published")
        .description("Number of payment outbox messages published successfully")
        .tag(TAG_EVENT_TYPE, eventType)
        .register(meterRegistry)
        .increment();
  }

  @Override
  public void outboxMessagePublicationFailed(String eventType, String failure) {
    Counter.builder("hotelbooking.payment.outbox.publication.failed")
        .description("Number of payment outbox publication failures")
        .tag(TAG_EVENT_TYPE, eventType)
        .tag(TAG_FAILURE, failure)
        .register(meterRegistry)
        .increment();
  }
}
