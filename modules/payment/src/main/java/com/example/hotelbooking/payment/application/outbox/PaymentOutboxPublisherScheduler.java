package com.example.hotelbooking.payment.application.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("payment-outbox-publisher")
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.payment.outbox.polling",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
class PaymentOutboxPublisherScheduler {

  private final PaymentOutboxPollingService pollingService;

  @Scheduled(fixedDelayString = "${app.payment.outbox.polling.fixed-delay-ms:5000}")
  void publishPendingMessages() {
    int publishedMessages = pollingService.publishPendingMessages();

    if (publishedMessages > 0) {
      log.info("Processed payment outbox messages: count={}", publishedMessages);
    }
  }
}
