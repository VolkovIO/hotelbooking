package com.example.hotelbooking.booking.adapter.out.persistence.postgres;

import com.example.hotelbooking.booking.application.event.BookingOutboxMessage;
import com.example.hotelbooking.booking.application.event.BookingOutboxStatus;
import com.example.hotelbooking.booking.application.port.out.BookingOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("booking-postgres")
@RequiredArgsConstructor
class JdbcBookingOutboxRepository implements BookingOutboxRepository {

  private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};

  private static final String PARAM_PROCESSING_STATUS = "processingStatus";

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
        .param("status", BookingOutboxStatus.NEW.name())
        .param("nextAttemptAt", Timestamp.from(message.occurredAt()))
        .param("occurredAt", Timestamp.from(message.occurredAt()))
        .update();
  }

  @Override
  @Transactional
  public List<BookingOutboxMessage> claimBatchForProcessing(
      int batchSize, Instant now, String lockedBy) {
    // SKIP LOCKED lets multiple service instances claim different rows safely.
    return jdbcClient
        .sql(
            """
            with candidate as (
                select id
                from booking_outbox
                where status = :newStatus
                  and next_attempt_at <= :now
                order by occurred_at
                for update skip locked
                limit :batchSize
            )
            update booking_outbox outbox
            set status = :processingStatus,
                locked_at = :now,
                locked_by = :lockedBy,
                updated_at = :now
            from candidate
            where outbox.id = candidate.id
            returning
                outbox.id,
                outbox.aggregate_type,
                outbox.aggregate_id,
                outbox.event_type,
                outbox.event_version,
                outbox.payload,
                outbox.occurred_at
            """)
        .param("newStatus", BookingOutboxStatus.NEW.name())
        .param(PARAM_PROCESSING_STATUS, BookingOutboxStatus.PROCESSING.name())
        .param("now", Timestamp.from(now))
        .param("lockedBy", lockedBy)
        .param("batchSize", batchSize)
        .query(
            (rs, rowNum) ->
                new BookingOutboxMessage(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("aggregate_type"),
                    UUID.fromString(rs.getString("aggregate_id")),
                    rs.getString("event_type"),
                    rs.getInt("event_version"),
                    deserializePayload(rs.getString("payload")),
                    rs.getTimestamp("occurred_at").toInstant()))
        .list();
  }

  @Override
  public void markPublished(UUID messageId, Instant publishedAt) {
    jdbcClient
        .sql(
            """
            update booking_outbox
            set status = :publishedStatus,
                published_at = :publishedAt,
                locked_at = null,
                locked_by = null,
                last_error = null,
                updated_at = :publishedAt
            where id = :id
              and status = :processingStatus
            """)
        .param("publishedStatus", BookingOutboxStatus.PUBLISHED.name())
        .param(PARAM_PROCESSING_STATUS, BookingOutboxStatus.PROCESSING.name())
        .param("publishedAt", Timestamp.from(publishedAt))
        .param("id", messageId)
        .update();
  }

  @Override
  public void markRetryableFailure(UUID messageId, Instant nextAttemptAt, String errorMessage) {
    Instant now = Instant.now();

    jdbcClient
        .sql(
            """
            update booking_outbox
            set status = :newStatus,
                attempts = attempts + 1,
                next_attempt_at = :nextAttemptAt,
                locked_at = null,
                locked_by = null,
                last_error = :lastError,
                updated_at = :now
            where id = :id
              and status = :processingStatus
            """)
        .param("newStatus", BookingOutboxStatus.NEW.name())
        .param(PARAM_PROCESSING_STATUS, BookingOutboxStatus.PROCESSING.name())
        .param("nextAttemptAt", Timestamp.from(nextAttemptAt))
        .param("lastError", errorMessage)
        .param("now", Timestamp.from(now))
        .param("id", messageId)
        .update();
  }

  @Override
  public void markTerminalFailure(UUID messageId, String errorMessage) {
    Instant now = Instant.now();

    jdbcClient
        .sql(
            """
            update booking_outbox
            set status = :failedStatus,
                attempts = attempts + 1,
                locked_at = null,
                locked_by = null,
                last_error = :lastError,
                updated_at = :now
            where id = :id
              and status = :processingStatus
            """)
        .param("failedStatus", BookingOutboxStatus.FAILED.name())
        .param(PARAM_PROCESSING_STATUS, BookingOutboxStatus.PROCESSING.name())
        .param("lastError", errorMessage)
        .param("now", Timestamp.from(now))
        .param("id", messageId)
        .update();
  }

  private String serializePayload(BookingOutboxMessage message) {
    try {
      return objectMapper.writeValueAsString(message.payload());
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize booking outbox payload", exception);
    }
  }

  private Map<String, Object> deserializePayload(String payload) {
    try {
      return objectMapper.readValue(payload, PAYLOAD_TYPE);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize booking outbox payload", exception);
    }
  }
}
