package com.example.hotelbooking.notification.adapter.in.kafka;

import com.example.hotelbooking.notification.application.event.BookingEventEnvelope;
import com.example.hotelbooking.notification.application.event.BookingEventHandlingResult;
import com.example.hotelbooking.notification.application.event.BookingEventRejectedException;
import com.example.hotelbooking.notification.application.service.BookingEventNotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("notification-kafka")
@RequiredArgsConstructor
class BookingEventsKafkaConsumer {

  private final ObjectMapper objectMapper;
  private final BookingEventNotificationService notificationService;

  @KafkaListener(
      topics = "${app.notification.kafka.booking-events-topic}",
      groupId = "${app.notification.kafka.group-id}")
  public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
    try {
      BookingEventEnvelope event =
          objectMapper.readValue(record.value(), BookingEventEnvelope.class);
      BookingEventHandlingResult result = notificationService.handle(event);

      logProcessedEvent(record, event, result);
      acknowledgment.acknowledge();
    } catch (JsonProcessingException exception) {
      log.warn(
          "Skipping malformed booking event: topic={}, partition={}, offset={}",
          record.topic(),
          record.partition(),
          record.offset(),
          exception);
      acknowledgment.acknowledge();
    } catch (BookingEventRejectedException exception) {
      log.warn(
          "Skipping rejected booking event: topic={}, partition={}, offset={}, reason={}",
          record.topic(),
          record.partition(),
          record.offset(),
          exception.getMessage());
      acknowledgment.acknowledge();
    }
  }

  private void logProcessedEvent(
      ConsumerRecord<String, String> record,
      BookingEventEnvelope event,
      BookingEventHandlingResult result) {
    if (result == BookingEventHandlingResult.CREATED) {
      log.info(
          "Created notification from booking event: topic={}, partition={}, offset={}, eventId={}, eventType={}",
          record.topic(),
          record.partition(),
          record.offset(),
          event.eventId(),
          event.eventType());
      return;
    }

    if (result == BookingEventHandlingResult.DUPLICATE) {
      log.info(
          "Booking event was already processed: topic={}, partition={}, offset={}, eventId={}, eventType={}",
          record.topic(),
          record.partition(),
          record.offset(),
          event.eventId(),
          event.eventType());
      return;
    }

    log.info(
        "Ignoring unsupported booking event: topic={}, partition={}, offset={}, eventId={}, eventType={}",
        record.topic(),
        record.partition(),
        record.offset(),
        event.eventId(),
        event.eventType());
  }
}
