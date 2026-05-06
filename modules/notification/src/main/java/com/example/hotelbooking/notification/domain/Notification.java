package com.example.hotelbooking.notification.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

@Getter
public final class Notification {

  private final NotificationId id;
  private final SourceEventId sourceEventId;
  private final String sourceEventType;
  private final NotificationType type;
  private final NotificationUserId userId;
  private final NotificationChannel channel;
  private final NotificationDestination destination;
  private final NotificationSubject subject;
  private final NotificationBody body;
  private NotificationStatus status;
  private NotificationAttemptCount attempts;
  private Instant nextAttemptAt;
  private NotificationErrorMessage lastError;
  private final Instant createdAt;
  private Instant sentAt;
  private Instant updatedAt;

  private Notification(
      NotificationId id,
      SourceEventId sourceEventId,
      String sourceEventType,
      NotificationType type,
      NotificationUserId userId,
      NotificationChannel channel,
      NotificationDestination destination,
      NotificationSubject subject,
      NotificationBody body,
      NotificationStatus status,
      NotificationAttemptCount attempts,
      Instant nextAttemptAt,
      NotificationErrorMessage lastError,
      Instant createdAt,
      Instant sentAt,
      Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.sourceEventId = Objects.requireNonNull(sourceEventId, "sourceEventId must not be null");
    this.sourceEventType = requireText(sourceEventType, "sourceEventType");
    this.type = Objects.requireNonNull(type, "type must not be null");
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.channel = Objects.requireNonNull(channel, "channel must not be null");
    this.destination = Objects.requireNonNull(destination, "destination must not be null");
    this.subject = Objects.requireNonNull(subject, "subject must not be null");
    this.body = Objects.requireNonNull(body, "body must not be null");
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.attempts = Objects.requireNonNull(attempts, "attempts must not be null");
    this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt must not be null");
    this.lastError = lastError;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    this.sentAt = sentAt;
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
  }

  public static Notification pending(
      SourceEventId sourceEventId,
      String sourceEventType,
      NotificationType type,
      NotificationUserId userId,
      NotificationChannel channel,
      NotificationDestination destination,
      NotificationSubject subject,
      NotificationBody body) {
    Instant now = Instant.now();

    return new Notification(
        NotificationId.newId(),
        sourceEventId,
        sourceEventType,
        type,
        userId,
        channel,
        destination,
        subject,
        body,
        NotificationStatus.PENDING,
        NotificationAttemptCount.zero(),
        now,
        null,
        now,
        null,
        now);
  }

  public static Notification skipped(
      SourceEventId sourceEventId,
      String sourceEventType,
      NotificationType type,
      NotificationUserId userId,
      NotificationSubject subject,
      NotificationBody body,
      String reason) {
    Instant now = Instant.now();

    return new Notification(
        NotificationId.newId(),
        sourceEventId,
        sourceEventType,
        type,
        userId,
        NotificationChannel.EMAIL,
        new NotificationDestination("skipped"),
        subject,
        body,
        NotificationStatus.SKIPPED,
        NotificationAttemptCount.zero(),
        now,
        new NotificationErrorMessage(reason),
        now,
        null,
        now);
  }

  public static Notification restore(
      NotificationId id,
      SourceEventId sourceEventId,
      String sourceEventType,
      NotificationType type,
      NotificationUserId userId,
      NotificationChannel channel,
      NotificationDestination destination,
      NotificationSubject subject,
      NotificationBody body,
      NotificationStatus status,
      NotificationAttemptCount attempts,
      Instant nextAttemptAt,
      NotificationErrorMessage lastError,
      Instant createdAt,
      Instant sentAt,
      Instant updatedAt) {
    return new Notification(
        id,
        sourceEventId,
        sourceEventType,
        type,
        userId,
        channel,
        destination,
        subject,
        body,
        status,
        attempts,
        nextAttemptAt,
        lastError,
        createdAt,
        sentAt,
        updatedAt);
  }

  @SuppressWarnings("PMD.NullAssignment")
  public void markSent() {
    this.status = NotificationStatus.SENT;
    this.sentAt = Instant.now();
    this.lastError = null;
    this.updatedAt = Instant.now();
  }

  public void markRetryableFailure(String errorMessage, Duration retryDelay) {
    this.status = NotificationStatus.PENDING;
    this.attempts = attempts.increment();
    this.nextAttemptAt = Instant.now().plus(retryDelay);
    this.lastError = new NotificationErrorMessage(errorMessage);
    this.updatedAt = Instant.now();
  }

  public void markFailed(String errorMessage) {
    this.status = NotificationStatus.FAILED;
    this.attempts = attempts.increment();
    this.lastError = new NotificationErrorMessage(errorMessage);
    this.updatedAt = Instant.now();
  }

  public boolean isPending() {
    return status == NotificationStatus.PENDING;
  }

  public boolean reachedMaxAttempts(int maxAttempts) {
    return attempts.value() + 1 >= maxAttempts;
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    return value;
  }
}
