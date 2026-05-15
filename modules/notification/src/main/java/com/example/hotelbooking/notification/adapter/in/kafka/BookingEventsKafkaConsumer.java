package com.example.hotelbooking.notification.adapter.in.kafka;

import com.example.hotelbooking.notification.application.event.BookingEventEnvelope;
import com.example.hotelbooking.notification.application.event.BookingEventHandlingResult;
import com.example.hotelbooking.notification.application.event.BookingEventRejectedException;
import com.example.hotelbooking.notification.application.port.out.NotificationMetrics;
import com.example.hotelbooking.notification.application.port.out.NotificationObservabilityContext;
import com.example.hotelbooking.notification.application.service.BookingEventNotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
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

  private static final String EVENT_TYPE_UNKNOWN = "unknown";
  private static final String OUTCOME_MALFORMED = "malformed";
  private static final String OUTCOME_REJECTED = "rejected";

  private final ObjectMapper objectMapper;
  private final BookingEventNotificationService notificationService;
  private final NotificationObservabilityContext observabilityContext;
  private final NotificationMetrics notificationMetrics;

  @KafkaListener(
      topics = "${app.notification.kafka.booking-events-topic}",
      groupId = "${app.notification.kafka.group-id}")
  public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
    BookingEventEnvelope event;

    try {
      event = objectMapper.readValue(record.value(), BookingEventEnvelope.class);
    } catch (JsonProcessingException exception) {
      notificationMetrics.bookingEventProcessed(EVENT_TYPE_UNKNOWN, OUTCOME_MALFORMED);

      log.warn(
          "Skipping malformed booking event: topic={}, partition={}, offset={}",
          record.topic(),
          record.partition(),
          record.offset(),
          exception);

      acknowledgment.acknowledge();
      return;
    }

    try (NotificationObservabilityContext.ContextScope ignored =
        observabilityContext.openBookingEvent(event)) {
      try {
        BookingEventHandlingResult result = notificationService.handle(event);

        notificationMetrics.bookingEventProcessed(event.eventType(), outcome(result));
        logProcessedEvent(record, event, result);
      } catch (BookingEventRejectedException exception) {
        notificationMetrics.bookingEventProcessed(event.eventType(), OUTCOME_REJECTED);

        log.warn(
            "Skipping rejected booking event: topic={}, partition={}, offset={}, eventId={}, eventType={}, reason={}",
            record.topic(),
            record.partition(),
            record.offset(),
            event.eventId(),
            event.eventType(),
            exception.getMessage());
      }

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

  private String outcome(BookingEventHandlingResult result) {
    return result.name().toLowerCase(Locale.ROOT);
  }
}
