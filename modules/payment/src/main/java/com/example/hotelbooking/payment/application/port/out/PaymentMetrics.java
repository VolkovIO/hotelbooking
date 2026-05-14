package com.example.hotelbooking.payment.application.port.out;

/**
 * Port for recording payment application metrics.
 *
 * <p>The payment module does not depend directly on Micrometer. It only reports business-level
 * payment and outbox outcomes. The runnable application decides how these signals are exported.
 */
public interface PaymentMetrics {

  static PaymentMetrics noop() {
    return new PaymentMetrics() {};
  }

  default void authorizationProcessed(String outcome) {}

  default void approvalProcessed(String outcome) {}

  default void cancellationProcessed(String outcome) {}

  default void outboxMessagePublished(String eventType) {}

  default void outboxMessagePublicationFailed(String eventType, String failure) {}
}
