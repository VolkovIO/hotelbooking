package com.example.hotelbooking.booking.adapter.out.persistence.postgres;

import com.example.hotelbooking.booking.application.event.BookingOutboxMessage;
import com.example.hotelbooking.booking.application.port.out.BookingOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@Profile("booking-postgres")
@RequiredArgsConstructor
class JdbcBookingOutboxRepository implements BookingOutboxRepository {

  private static final String NEW_STATUS = "NEW";

  private final JdbcClient jdbcClient;
  private final ObjectMapper objectMapper;

  @Override
  public void save(BookingOutboxMessage message) {
    jdbcClient
        .sql(
            """
            insert into booking_outbox (
                id,
                aggregate_type,
                aggregate_id,
                event_type,
                event_version,
                payload,
                status,
                attempts,
                next_attempt_at,
                occurred_at
            )
            values (
                :id,
                :aggregateType,
                :aggregateId,
                :eventType,
                :eventVersion,
                cast(:payload as jsonb),
                :status,
                0,
                :nextAttemptAt,
                :occurredAt
            )
            """)
        .param("id", message.id())
        .param("aggregateType", message.aggregateType())
        .param("aggregateId", message.aggregateId())
        .param("eventType", message.eventType())
        .param("eventVersion", message.eventVersion())
        .param("payload", serializePayload(message))
        .param("status", NEW_STATUS)
        .param("nextAttemptAt", Timestamp.from(message.occurredAt()))
        .param("occurredAt", Timestamp.from(message.occurredAt()))
        .update();
  }

  private String serializePayload(BookingOutboxMessage message) {
    try {
      return objectMapper.writeValueAsString(message.payload());
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize booking outbox payload", exception);
    }
  }
}
