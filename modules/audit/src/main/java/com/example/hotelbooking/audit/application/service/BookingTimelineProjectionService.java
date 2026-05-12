package com.example.hotelbooking.audit.application.service;

import com.example.hotelbooking.audit.application.event.BookingEventEnvelope;
import com.example.hotelbooking.audit.application.event.PaymentEventEnvelope;
import com.example.hotelbooking.audit.application.event.TimelineEventHandlingResult;
import com.example.hotelbooking.audit.application.port.out.TimelineEventRepository;
import com.example.hotelbooking.audit.domain.TimelineEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingTimelineProjectionService {

  private static final String BOOKING_EVENT_SOURCE = "booking-service";
  private static final String PAYMENT_EVENT_SOURCE = "payment-service";
  private static final String BOOKING_ID_PAYLOAD_FIELD = "bookingId";

  private final TimelineEventRepository timelineEventRepository;
  private final Clock clock;

  public TimelineEventHandlingResult handle(BookingEventEnvelope event) {
    TimelineEvent timelineEvent =
        new TimelineEvent(
            event.eventId(),
            event.eventType(),
            event.eventVersion(),
            BOOKING_EVENT_SOURCE,
            event.aggregateType(),
            event.aggregateId(),
            event.aggregateId(),
            event.occurredAt(),
            event.correlationId(),
            event.causationId(),
            event.payload(),
            Instant.now(clock));

    return save(timelineEvent);
  }

  public TimelineEventHandlingResult handle(PaymentEventEnvelope event) {
    UUID bookingId = bookingIdFromPayload(event.payload());

    TimelineEvent timelineEvent =
        new TimelineEvent(
            event.eventId(),
            event.eventType(),
            event.eventVersion(),
            PAYMENT_EVENT_SOURCE,
            event.aggregateType(),
            event.aggregateId(),
            bookingId,
            event.occurredAt(),
            event.correlationId(),
            event.causationId(),
            event.payload(),
            Instant.now(clock));

    return save(timelineEvent);
  }

  private TimelineEventHandlingResult save(TimelineEvent timelineEvent) {
    boolean created = timelineEventRepository.saveIfAbsent(timelineEvent);

    if (created) {
      return TimelineEventHandlingResult.CREATED;
    }

    return TimelineEventHandlingResult.DUPLICATE;
  }

  private UUID bookingIdFromPayload(Map<String, Object> payload) {
    Object bookingIdValue = payload.get(BOOKING_ID_PAYLOAD_FIELD);

    if (bookingIdValue instanceof UUID bookingId) {
      return bookingId;
    }

    if (bookingIdValue instanceof String bookingId) {
      return UUID.fromString(bookingId);
    }

    throw new IllegalArgumentException(
        "Payment event payload does not contain valid bookingId field");
  }
}
