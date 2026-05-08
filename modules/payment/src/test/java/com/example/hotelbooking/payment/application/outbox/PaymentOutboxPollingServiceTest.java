package com.example.hotelbooking.payment.application.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hotelbooking.payment.application.port.out.PaymentOutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxPollingServiceTest {

  private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID AGGREGATE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID CORRELATION_ID =
      UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
  private static final String EVENT_TYPE = "PaymentAuthorized";
  private static final String AGGREGATE_TYPE = "Payment";
  private static final String PAYMENT_ID = "paymentId";
  private static final String KAFKA_UNAVAILABLE_ERROR = "kafka unavailable";
  private static final String TEST_ERROR_MESSAGE = "test";
  private static final int BATCH_SIZE = 20;
  private static final int MAX_ATTEMPTS = 3;
  private static final int INITIAL_RETRY_COUNT = 0;
  private static final int LAST_RETRY_COUNT = 2;

  @Mock private PaymentOutboxRepository paymentOutboxRepository;

  @Mock private PaymentOutboxEventPublisher paymentOutboxEventPublisher;

  @Test
  void shouldPublishClaimedMessages() throws PaymentOutboxPublicationException {
    PaymentOutboxPollingProperties properties = properties();
    PaymentOutboxPollingService service = newService(properties);
    PaymentOutboxMessage message = message(INITIAL_RETRY_COUNT);

    when(paymentOutboxRepository.claimBatchForProcessing(any(Instant.class), eq(BATCH_SIZE)))
        .thenReturn(List.of(message));

    service.publishPendingMessages();

    verify(paymentOutboxEventPublisher).publish(message);
    verify(paymentOutboxRepository).markPublished(eq(EVENT_ID), any(Instant.class));
  }

  @Test
  void shouldMarkRetryableFailure() throws PaymentOutboxPublicationException {
    PaymentOutboxPollingProperties properties = properties();
    PaymentOutboxPollingService service = newService(properties);
    PaymentOutboxMessage message = message(INITIAL_RETRY_COUNT);

    when(paymentOutboxRepository.claimBatchForProcessing(any(Instant.class), eq(BATCH_SIZE)))
        .thenReturn(List.of(message));
    doThrow(
            new PaymentOutboxPublicationException(
                KAFKA_UNAVAILABLE_ERROR, new RuntimeException(TEST_ERROR_MESSAGE)))
        .when(paymentOutboxEventPublisher)
        .publish(message);

    service.publishPendingMessages();

    verify(paymentOutboxRepository)
        .markRetryableFailure(eq(EVENT_ID), eq(KAFKA_UNAVAILABLE_ERROR), any(Instant.class));
  }

  @Test
  void shouldMarkTerminalFailureWhenMaxAttemptsReached() throws PaymentOutboxPublicationException {
    PaymentOutboxPollingProperties properties = properties();
    PaymentOutboxPollingService service = newService(properties);
    PaymentOutboxMessage message = message(LAST_RETRY_COUNT);

    when(paymentOutboxRepository.claimBatchForProcessing(any(Instant.class), eq(BATCH_SIZE)))
        .thenReturn(List.of(message));
    doThrow(
            new PaymentOutboxPublicationException(
                KAFKA_UNAVAILABLE_ERROR, new RuntimeException(TEST_ERROR_MESSAGE)))
        .when(paymentOutboxEventPublisher)
        .publish(message);

    service.publishPendingMessages();

    verify(paymentOutboxRepository).markTerminalFailure(EVENT_ID, KAFKA_UNAVAILABLE_ERROR);
  }

  private PaymentOutboxPollingService newService(PaymentOutboxPollingProperties properties) {
    return new PaymentOutboxPollingService(
        paymentOutboxRepository, paymentOutboxEventPublisher, properties);
  }

  private PaymentOutboxPollingProperties properties() {
    PaymentOutboxPollingProperties properties = new PaymentOutboxPollingProperties();

    properties.setBatchSize(BATCH_SIZE);
    properties.setMaxAttempts(MAX_ATTEMPTS);
    properties.setRetryDelay(Duration.ofSeconds(30));

    return properties;
  }

  private PaymentOutboxMessage message(int retryCount) {
    Instant now = Instant.now();

    return new PaymentOutboxMessage(
        EVENT_ID,
        EVENT_TYPE,
        1,
        AGGREGATE_TYPE,
        AGGREGATE_ID,
        Map.of(PAYMENT_ID, AGGREGATE_ID),
        now,
        CORRELATION_ID,
        null,
        PaymentOutboxStatus.NEW,
        retryCount,
        now,
        null,
        now,
        null,
        now);
  }
}
