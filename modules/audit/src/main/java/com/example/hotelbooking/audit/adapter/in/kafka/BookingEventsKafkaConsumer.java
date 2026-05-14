package com.example.hotelbooking.audit.adapter.in.kafka;

import com.example.hotelbooking.audit.application.event.BookingEventEnvelope;
import com.example.hotelbooking.audit.application.event.TimelineEventHandlingResult;
import com.example.hotelbooking.audit.application.port.out.AuditObservabilityContext;
import com.example.hotelbooking.audit.application.service.BookingTimelineProjectionService;
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
@Profile("audit-kafka")
@RequiredArgsConstructor
class BookingEventsKafkaConsumer {

  private final ObjectMapper objectMapper;
  private final BookingTimelineProjectionService projectionService;
  private final AuditObservabilityContext observabilityContext;

  @KafkaListener(
      topics = "${app.audit.kafka.booking-events-topic}",
      groupId = "${app.audit.kafka.group-id}")
  public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
    BookingEventEnvelope event;

    try {
      event = objectMapper.readValue(record.value(), BookingEventEnvelope.class);
    } catch (JsonProcessingException exception) {
      log.warn(
          "Skipping malformed booking timeline event: topic={}, partition={}, offset={}",
          record.topic(),
          record.partition(),
          record.offset(),
          exception);

      acknowledgment.acknowledge();
      return;
    }

    try (AuditObservabilityContext.ContextScope ignored =
        observabilityContext.openBookingEvent(event)) {
      TimelineEventHandlingResult result = projectionService.handle(event);

      logProcessedEvent(record, event, result);
      acknowledgment.acknowledge();
    }
  }

  private void logProcessedEvent(
      ConsumerRecord<String, String> record,
      BookingEventEnvelope event,
      TimelineEventHandlingResult result) {
    if (result == TimelineEventHandlingResult.CREATED) {
      log.info(
          "Created booking timeline event: "
              + "topic={}, partition={}, offset={}, eventId={}, eventType={}, bookingId={}, correlationId={}",
          record.topic(),
          record.partition(),
          record.offset(),
          event.eventId(),
          event.eventType(),
          event.aggregateId(),
          event.correlationId());

      return;
    }

    log.info(
        "Booking timeline event already exists: "
            + "topic={}, partition={}, offset={}, eventId={}, eventType={}, bookingId={}, correlationId={}",
        record.topic(),
        record.partition(),
        record.offset(),
        event.eventId(),
        event.eventType(),
        event.aggregateId(),
        event.correlationId());
  }
}
