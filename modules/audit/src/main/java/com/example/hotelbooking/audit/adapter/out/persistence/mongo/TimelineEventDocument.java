package com.example.hotelbooking.audit.adapter.out.persistence.mongo;

import com.example.hotelbooking.audit.domain.TimelineEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document(collection = "timelineEvents")
@CompoundIndex(
    name = "booking_timeline_order_idx",
    def = "{'bookingId': 1, 'occurredAt': 1, 'recordedAt': 1}")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
class TimelineEventDocument {

  @Id private String id;

  private UUID eventId;

  private UUID bookingId;

  private String eventType;

  private int eventVersion;

  private String source;

  private String aggregateType;

  private UUID aggregateId;

  private Instant occurredAt;

  private UUID correlationId;

  private UUID causationId;

  private Map<String, Object> payload;

  private Instant recordedAt;

  static TimelineEventDocument fromDomain(TimelineEvent event) {
    return new TimelineEventDocument(
        event.eventId().toString(),
        event.eventId(),
        event.bookingId(),
        event.eventType(),
        event.eventVersion(),
        event.source(),
        event.aggregateType(),
        event.aggregateId(),
        event.occurredAt(),
        event.correlationId(),
        event.causationId(),
        event.payload(),
        event.recordedAt());
  }

  TimelineEvent toDomain() {
    return new TimelineEvent(
        eventId,
        eventType,
        eventVersion,
        source,
        aggregateType,
        aggregateId,
        bookingId,
        occurredAt,
        correlationId,
        causationId,
        payload,
        recordedAt);
  }
}
