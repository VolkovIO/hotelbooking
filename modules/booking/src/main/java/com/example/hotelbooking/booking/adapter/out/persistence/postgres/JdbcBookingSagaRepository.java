package com.example.hotelbooking.booking.adapter.out.persistence.postgres;

import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaFailureReason;
import com.example.hotelbooking.booking.application.saga.BookingSagaId;
import com.example.hotelbooking.booking.application.saga.BookingSagaStatus;
import com.example.hotelbooking.booking.application.saga.BookingSagaStep;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@Profile("booking-postgres")
@RequiredArgsConstructor
class JdbcBookingSagaRepository implements BookingSagaRepository {

  private final JdbcClient jdbcClient;

  @Override
  public BookingSaga save(BookingSaga saga) {
    jdbcClient
        .sql(
            """
            insert into booking_sagas (
                id,
                booking_id,
                payment_amount,
                payment_currency,
                status,
                current_step,
                payment_id,
                last_failure_reason,
                retry_count,
                next_attempt_at,
                created_at,
                completed_at,
                compensated_at,
                failed_at,
                updated_at
            )
            values (
                :id,
                :bookingId,
                :paymentAmount,
                :paymentCurrency,
                :status,
                :currentStep,
                :paymentId,
                :lastFailureReason,
                :retryCount,
                :nextAttemptAt,
                :createdAt,
                :completedAt,
                :compensatedAt,
                :failedAt,
                :updatedAt
            )
            on conflict (id) do update
               set booking_id = excluded.booking_id,
                   payment_amount = excluded.payment_amount,
                   payment_currency = excluded.payment_currency,
                   status = excluded.status,
                   current_step = excluded.current_step,
                   payment_id = excluded.payment_id,
                   last_failure_reason = excluded.last_failure_reason,
                   retry_count = excluded.retry_count,
                   next_attempt_at = excluded.next_attempt_at,
                   completed_at = excluded.completed_at,
                   compensated_at = excluded.compensated_at,
                   failed_at = excluded.failed_at,
                   updated_at = excluded.updated_at
            """)
        .param("id", saga.getId().value())
        .param("bookingId", saga.getBookingId())
        .param("paymentAmount", saga.getPaymentAmount())
        .param("paymentCurrency", saga.getPaymentCurrency())
        .param("status", saga.getStatus().name())
        .param("currentStep", saga.getCurrentStep().name())
        .param("paymentId", saga.getPaymentId())
        .param("lastFailureReason", failureReasonValue(saga))
        .param("retryCount", saga.getRetryCount())
        .param("nextAttemptAt", localDateTime(saga.getNextAttemptAt()))
        .param("createdAt", localDateTime(saga.getCreatedAt()))
        .param("completedAt", localDateTime(saga.getCompletedAt()))
        .param("compensatedAt", localDateTime(saga.getCompensatedAt()))
        .param("failedAt", localDateTime(saga.getFailedAt()))
        .param("updatedAt", localDateTime(saga.getUpdatedAt()))
        .update();

    return saga;
  }

  @Override
  public Optional<BookingSaga> findById(BookingSagaId sagaId) {
    return jdbcClient
        .sql(
            """
            select id,
                   booking_id,
                   payment_amount,
                   payment_currency,
                   status,
                   current_step,
                   payment_id,
                   last_failure_reason,
                   retry_count,
                   next_attempt_at,
                   created_at,
                   completed_at,
                   compensated_at,
                   failed_at,
                   updated_at
              from booking_sagas
             where id = :id
            """)
        .param("id", sagaId.value())
        .query(this::mapRow)
        .optional();
  }

  @Override
  public Optional<BookingSaga> findByBookingId(UUID bookingId) {
    return jdbcClient
        .sql(
            """
            select id,
                   booking_id,
                   payment_amount,
                   payment_currency,
                   status,
                   current_step,
                   payment_id,
                   last_failure_reason,
                   retry_count,
                   next_attempt_at,
                   created_at,
                   completed_at,
                   compensated_at,
                   failed_at,
                   updated_at
              from booking_sagas
             where booking_id = :bookingId
            """)
        .param("bookingId", bookingId)
        .query(this::mapRow)
        .optional();
  }

  @Override
  public List<BookingSaga> findReadyForRetry(Instant now, int batchSize) {
    return jdbcClient
        .sql(
            """
            select id,
                   booking_id,
                   payment_amount,
                   payment_currency,
                   status,
                   current_step,
                   payment_id,
                   last_failure_reason,
                   retry_count,
                   next_attempt_at,
                   created_at,
                   completed_at,
                   compensated_at,
                   failed_at,
                   updated_at
              from booking_sagas
             where status = :status
               and next_attempt_at <= :now
             order by updated_at
             limit :batchSize
            """)
        .param("status", BookingSagaStatus.WAITING_RETRY.name())
        .param("now", localDateTime(now))
        .param("batchSize", batchSize)
        .query(this::mapRow)
        .list();
  }

  @SuppressWarnings("unused")
  private BookingSaga mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
    return BookingSaga.restore(
        new BookingSagaId(resultSet.getObject("id", UUID.class)),
        resultSet.getObject("booking_id", UUID.class),
        resultSet.getBigDecimal("payment_amount"),
        resultSet.getString("payment_currency"),
        BookingSagaStatus.valueOf(resultSet.getString("status")),
        BookingSagaStep.valueOf(resultSet.getString("current_step")),
        resultSet.getObject("payment_id", UUID.class),
        failureReason(resultSet.getString("last_failure_reason")),
        resultSet.getInt("retry_count"),
        instant(resultSet, "next_attempt_at"),
        instant(resultSet, "created_at"),
        instantOrNull(resultSet, "completed_at"),
        instantOrNull(resultSet, "compensated_at"),
        instantOrNull(resultSet, "failed_at"),
        instant(resultSet, "updated_at"));
  }

  private String failureReasonValue(BookingSaga saga) {
    if (saga.getLastFailureReason() == null) {
      return null;
    }

    return saga.getLastFailureReason().value();
  }

  private BookingSagaFailureReason failureReason(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return new BookingSagaFailureReason(value);
  }

  private LocalDateTime localDateTime(Instant instant) {
    if (instant == null) {
      return null;
    }

    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private Instant instant(ResultSet resultSet, String columnName) throws SQLException {
    return resultSet.getObject(columnName, LocalDateTime.class).toInstant(ZoneOffset.UTC);
  }

  private Instant instantOrNull(ResultSet resultSet, String columnName) throws SQLException {
    LocalDateTime value = resultSet.getObject(columnName, LocalDateTime.class);

    if (value == null) {
      return null;
    }

    return value.toInstant(ZoneOffset.UTC);
  }
}
