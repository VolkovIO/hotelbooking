package com.example.hotelbooking.payment.adapter.out.messaging.kafka;

import com.example.hotelbooking.payment.application.outbox.PaymentEventEnvelope;
import com.example.hotelbooking.payment.application.outbox.PaymentOutboxEventPublisher;
import com.example.hotelbooking.payment.application.outbox.PaymentOutboxMessage;
import com.example.hotelbooking.payment.application.outbox.PaymentOutboxPublicationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("payment-outbox-kafka")
@RequiredArgsConstructor
class KafkaPaymentOutboxEventPublisher implements PaymentOutboxEventPublisher {

  private static final long SEND_TIMEOUT_SECONDS = 5L;

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.payment.kafka.payment-events-topic:payment.events}")
  private String topic;

  @Override
  public void publish(PaymentOutboxMessage message) throws PaymentOutboxPublicationException {
    try {
      String key = message.aggregateId().toString();
      String value = objectMapper.writeValueAsString(PaymentEventEnvelope.from(message));

      kafkaTemplate.send(topic, key, value).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (JsonProcessingException | ExecutionException | TimeoutException exception) {
      throw new PaymentOutboxPublicationException(
          "Failed to publish payment outbox event", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new PaymentOutboxPublicationException(
          "Payment outbox event publication was interrupted", exception);
    }
  }
}
