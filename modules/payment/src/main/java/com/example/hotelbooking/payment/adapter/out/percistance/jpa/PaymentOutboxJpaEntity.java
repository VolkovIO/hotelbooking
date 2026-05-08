package com.example.hotelbooking.payment.adapter.out.percistance.jpa;

import com.example.hotelbooking.payment.application.outbox.PaymentOutboxMessage;
import com.example.hotelbooking.payment.application.outbox.PaymentOutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "payment_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class PaymentOutboxJpaEntity {

  @Id
  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "event_type", nullable = false, length = 128)
  private String eventType;

  @Column(name = "event_version", nullable = false)
  private int eventVersion;

  @Column(name = "aggregate_type", nullable = false, length = 128)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "correlation_id", nullable = false)
  private UUID correlationId;

  @Column(name = "causation_id")
  private UUID causationId;

  @Column(name = "processing_status", nullable = false, length = 32)
  private String processingStatus;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  @Column(name = "last_error", length = 1000)
  private String lastError;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  static PaymentOutboxJpaEntity from(PaymentOutboxMessage message) {
    return new PaymentOutboxJpaEntity(
        message.eventId(),
        message.eventType(),
        message.eventVersion(),
        message.aggregateType(),
        message.aggregateId(),
        message.payload(),
        message.occurredAt(),
        message.correlationId(),
        message.causationId(),
        message.processingStatus().name(),
        message.retryCount(),
        message.nextAttemptAt(),
        message.lastError(),
        message.createdAt(),
        message.publishedAt(),
        message.updatedAt());
  }

  PaymentOutboxMessage toDomain() {
    return new PaymentOutboxMessage(
        eventId,
        eventType,
        eventVersion,
        aggregateType,
        aggregateId,
        payload,
        occurredAt,
        correlationId,
        causationId,
        PaymentOutboxStatus.valueOf(processingStatus),
        retryCount,
        nextAttemptAt,
        lastError,
        createdAt,
        publishedAt,
        updatedAt);
  }

  void markProcessing(Instant now) {
    this.processingStatus = PaymentOutboxStatus.PROCESSING.name();
    this.updatedAt = now;
  }

  void markPublished(Instant now) {
    this.processingStatus = PaymentOutboxStatus.PUBLISHED.name();
    this.publishedAt = now;
    this.updatedAt = now;
  }

  void markRetryableFailure(String errorMessage, Instant nextAttemptAt, Instant now) {
    this.processingStatus = PaymentOutboxStatus.NEW.name();
    this.retryCount++;
    this.nextAttemptAt = nextAttemptAt;
    this.lastError = errorMessage;
    this.updatedAt = now;
  }

  void markTerminalFailure(String errorMessage, Instant now) {
    this.processingStatus = PaymentOutboxStatus.FAILED.name();
    this.retryCount++;
    this.lastError = errorMessage;
    this.updatedAt = now;
  }
}
