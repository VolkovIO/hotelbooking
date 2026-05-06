package com.example.hotelbooking.booking.adapter.out.outbox.kafka;

import com.example.hotelbooking.booking.application.event.BookingEventEnvelope;
import com.example.hotelbooking.booking.application.event.BookingOutboxMessage;
import com.example.hotelbooking.booking.application.exception.BookingOutboxPublicationException;
import com.example.hotelbooking.booking.application.port.out.BookingOutboxEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("outbox-kafka")
@RequiredArgsConstructor
class KafkaBookingOutboxEventPublisher implements BookingOutboxEventPublisher {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.booking.outbox.kafka.topic-name:booking.events}")
  private String topicName;

  @Value("${app.booking.outbox.kafka.send-timeout:PT10S}")
  private Duration sendTimeout;

  @Override
  public void publish(BookingOutboxMessage message) throws BookingOutboxPublicationException {
    BookingEventEnvelope envelope = BookingEventEnvelope.from(message);
    String key = message.aggregateId().toString();
    String payload = serialize(envelope);

    try {
      kafkaTemplate
          .send(topicName, key, payload)
          .get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);

      log.info(
          "Published booking event to Kafka: topic={}, key={}, eventId={}, eventType={}",
          topicName,
          key,
          message.id(),
          message.eventType());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new BookingOutboxPublicationException(
          "Kafka event publication was interrupted", exception);
    } catch (ExecutionException | TimeoutException exception) {
      throw new BookingOutboxPublicationException(
          "Failed to publish booking event to Kafka", exception);
    }
  }

  private String serialize(BookingEventEnvelope envelope) throws BookingOutboxPublicationException {
    try {
      return objectMapper.writeValueAsString(envelope);
    } catch (JsonProcessingException exception) {
      throw new BookingOutboxPublicationException(
          "Failed to serialize booking event envelope", exception);
    }
  }
}
