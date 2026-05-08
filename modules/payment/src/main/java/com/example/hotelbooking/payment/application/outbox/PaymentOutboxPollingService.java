package com.example.hotelbooking.payment.application.outbox;

import com.example.hotelbooking.payment.application.port.out.PaymentOutboxRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("payment-outbox-publisher")
@RequiredArgsConstructor
public class PaymentOutboxPollingService {

  private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

  private final PaymentOutboxRepository paymentOutboxRepository;
  private final PaymentOutboxEventPublisher paymentOutboxEventPublisher;
  private final PaymentOutboxPollingProperties properties;

  public int publishPendingMessages() {
    List<PaymentOutboxMessage> messages =
        paymentOutboxRepository.claimBatchForProcessing(Instant.now(), properties.getBatchSize());

    messages.forEach(this::publishMessage);

    return messages.size();
  }

  private void publishMessage(PaymentOutboxMessage message) {
    try {
      paymentOutboxEventPublisher.publish(message);
      paymentOutboxRepository.markPublished(message.eventId(), Instant.now());
    } catch (PaymentOutboxPublicationException exception) {
      handlePublicationFailure(message, exception);
    }
  }

  private void handlePublicationFailure(
      PaymentOutboxMessage message, PaymentOutboxPublicationException exception) {
    String errorMessage = truncateErrorMessage(exception.getMessage());

    if (shouldRetry(message)) {
      paymentOutboxRepository.markRetryableFailure(
          message.eventId(), errorMessage, Instant.now().plus(properties.getRetryDelay()));
      return;
    }

    paymentOutboxRepository.markTerminalFailure(message.eventId(), errorMessage);
  }

  private boolean shouldRetry(PaymentOutboxMessage message) {
    return message.retryCount() + 1 < properties.getMaxAttempts();
  }

  private String truncateErrorMessage(String errorMessage) {
    if (errorMessage == null || errorMessage.length() <= MAX_ERROR_MESSAGE_LENGTH) {
      return errorMessage;
    }

    return errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
  }
}
