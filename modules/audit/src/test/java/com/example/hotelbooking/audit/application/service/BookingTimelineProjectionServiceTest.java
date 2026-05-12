package com.example.hotelbooking.audit.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hotelbooking.audit.application.event.PaymentEventEnvelope;
import com.example.hotelbooking.audit.application.event.TimelineEventHandlingResult;
import com.example.hotelbooking.audit.application.port.out.TimelineEventRepository;
import com.example.hotelbooking.audit.domain.TimelineEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingTimelineProjectionServiceTest {

  private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID PAYMENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID BOOKING_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final UUID CORRELATION_ID =
      UUID.fromString("40000000-0000-0000-0000-000000000001");
  private static final Instant OCCURRED_AT = Instant.parse("2035-01-10T10:00:00Z");
  private static final Instant RECORDED_AT = Instant.parse("2035-01-10T10:00:01Z");

  @Test
  void shouldProjectPaymentEventIntoBookingTimeline() {
    InMemoryTimelineEventRepository repository = new InMemoryTimelineEventRepository();
    BookingTimelineProjectionService service =
        new BookingTimelineProjectionService(repository, Clock.fixed(RECORDED_AT, ZoneOffset.UTC));

    PaymentEventEnvelope event =
        new PaymentEventEnvelope(
            EVENT_ID,
            "PaymentAuthorized",
            1,
            "Payment",
            PAYMENT_ID,
            OCCURRED_AT,
            CORRELATION_ID,
            null,
            Map.of(
                "paymentId", PAYMENT_ID.toString(),
                "bookingId", BOOKING_ID.toString(),
                "status", "AUTHORIZED"));

    TimelineEventHandlingResult result = service.handle(event);

    assertThat(result).isEqualTo(TimelineEventHandlingResult.CREATED);

    List<TimelineEvent> timeline = repository.findByBookingId(BOOKING_ID);

    assertThat(timeline).hasSize(1);

    TimelineEvent timelineEvent = timeline.getFirst();

    assertThat(timelineEvent.eventId()).isEqualTo(EVENT_ID);
    assertThat(timelineEvent.eventType()).isEqualTo("PaymentAuthorized");
    assertThat(timelineEvent.source()).isEqualTo("payment-service");
    assertThat(timelineEvent.aggregateType()).isEqualTo("Payment");
    assertThat(timelineEvent.aggregateId()).isEqualTo(PAYMENT_ID);
    assertThat(timelineEvent.bookingId()).isEqualTo(BOOKING_ID);
    assertThat(timelineEvent.correlationId()).isEqualTo(CORRELATION_ID);
    assertThat(timelineEvent.recordedAt()).isEqualTo(RECORDED_AT);
  }

  private static final class InMemoryTimelineEventRepository implements TimelineEventRepository {

    private final List<TimelineEvent> events = new ArrayList<>();

    @Override
    public boolean saveIfAbsent(TimelineEvent event) {
      boolean alreadyExists =
          events.stream()
              .anyMatch(existingEvent -> existingEvent.eventId().equals(event.eventId()));

      if (alreadyExists) {
        return false;
      }

      events.add(event);
      return true;
    }

    @Override
    public List<TimelineEvent> findByBookingId(UUID bookingId) {
      return events.stream().filter(event -> event.bookingId().equals(bookingId)).toList();
    }
  }
}
