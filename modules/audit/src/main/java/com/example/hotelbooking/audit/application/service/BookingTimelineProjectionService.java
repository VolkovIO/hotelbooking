package com.example.hotelbooking.audit.application.service;

import com.example.hotelbooking.audit.application.event.BookingEventEnvelope;
import com.example.hotelbooking.audit.application.event.TimelineEventHandlingResult;
import com.example.hotelbooking.audit.application.port.out.TimelineEventRepository;
import com.example.hotelbooking.audit.domain.TimelineEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingTimelineProjectionService {

  private static final String BOOKING_EVENT_SOURCE = "booking-service";

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
            bookingId(event),
            event.occurredAt(),
            event.correlationId(),
            event.causationId(),
            event.payload(),
            Instant.now(clock));

    boolean created = timelineEventRepository.saveIfAbsent(timelineEvent);

    if (created) {
      return TimelineEventHandlingResult.CREATED;
    }

    return TimelineEventHandlingResult.DUPLICATE;
  }

  private UUID bookingId(BookingEventEnvelope event) {
    return event.aggregateId();
  }
}
