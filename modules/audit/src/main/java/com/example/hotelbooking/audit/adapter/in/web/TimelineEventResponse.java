package com.example.hotelbooking.audit.adapter.in.web;

import com.example.hotelbooking.audit.domain.TimelineEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Schema(description = "Single event in the distributed booking timeline")
public record TimelineEventResponse(
    @Schema(
            description = "Unique event identifier used for idempotent timeline projection",
            example = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        UUID eventId,
    @Schema(description = "Integration event type", example = "BookingConfirmed") String eventType,
    @Schema(description = "Event schema version", example = "1") int eventVersion,
    @Schema(description = "Service that produced the event", example = "booking-service")
        String source,
    @Schema(description = "Aggregate type from the original integration event", example = "Booking")
        String aggregateType,
    @Schema(
            description = "Aggregate identifier from the original integration event",
            example = "11111111-1111-1111-1111-111111111111")
        UUID aggregateId,
    @Schema(
            description = "Booking identifier used to group timeline events",
            example = "11111111-1111-1111-1111-111111111111")
        UUID bookingId,
    @Schema(description = "When the original event happened", example = "2026-05-12T10:15:30Z")
        Instant occurredAt,
    @Schema(
            description = "Identifier used to correlate events belonging to one distributed flow",
            example = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
            nullable = true)
        UUID correlationId,
    @Schema(
            description = "Identifier of the event or command that caused this event",
            example = "cccccccc-cccc-cccc-cccc-cccccccccccc",
            nullable = true)
        UUID causationId,
    @Schema(description = "Original event payload") Map<String, Object> payload,
    @Schema(
            description = "When audit-service recorded this timeline event",
            example = "2026-05-12T10:15:31Z")
        Instant recordedAt) {

  public TimelineEventResponse {
    payload = Map.copyOf(Objects.requireNonNull(payload, "payload must not be null"));
  }

  @Override
  public Map<String, Object> payload() {
    return Map.copyOf(payload);
  }

  static TimelineEventResponse fromDomain(TimelineEvent event) {
    return new TimelineEventResponse(
        event.eventId(),
        event.eventType(),
        event.eventVersion(),
        event.source(),
        event.aggregateType(),
        event.aggregateId(),
        event.bookingId(),
        event.occurredAt(),
        event.correlationId(),
        event.causationId(),
        event.payload(),
        event.recordedAt());
  }
}
