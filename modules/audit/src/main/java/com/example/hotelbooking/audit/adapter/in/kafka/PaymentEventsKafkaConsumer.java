package com.example.hotelbooking.audit.adapter.in.kafka;

import com.example.hotelbooking.audit.application.event.PaymentEventEnvelope;
import com.example.hotelbooking.audit.application.event.TimelineEventHandlingResult;
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
class PaymentEventsKafkaConsumer {

  private final ObjectMapper objectMapper;
  private final BookingTimelineProjectionService projectionService;

  @KafkaListener(
      topics = "${app.audit.kafka.payment-events-topic}",
      groupId = "${app.audit.kafka.group-id}")
  public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
    try {
      PaymentEventEnvelope event =
          objectMapper.readValue(record.value(), PaymentEventEnvelope.class);

      TimelineEventHandlingResult result = projectionService.handle(event);

      logProcessedEvent(record, event, result);
      acknowledgment.acknowledge();
    } catch (JsonProcessingException | IllegalArgumentException exception) {
      log.warn(
          "Skipping invalid payment timeline event: topic={}, partition={}, offset={}",
          record.topic(),
          record.partition(),
          record.offset(),
          exception);

      acknowledgment.acknowledge();
    }
  }

  private void logProcessedEvent(
      ConsumerRecord<String, String> record,
      PaymentEventEnvelope event,
      TimelineEventHandlingResult result) {
    if (result == TimelineEventHandlingResult.CREATED) {
      log.info(
          "Created payment timeline event: topic={}, partition={}, offset={}, eventId={}, "
              + "eventType={}, paymentId={}, bookingId={}, correlationId={}",
          record.topic(),
          record.partition(),
          record.offset(),
          event.eventId(),
          event.eventType(),
          event.aggregateId(),
          event.payload().get("bookingId"),
          event.correlationId());

      return;
    }

    log.info(
        "Payment timeline event already exists: topic={}, partition={}, offset={}, eventId={}, "
            + "eventType={}, paymentId={}, bookingId={}, correlationId={}",
        record.topic(),
        record.partition(),
        record.offset(),
        event.eventId(),
        event.eventType(),
        event.aggregateId(),
        event.payload().get("bookingId"),
        event.correlationId());
  }
}
