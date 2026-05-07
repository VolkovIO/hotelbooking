package com.example.hotelbooking.notification.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

/**
 * Notification is a delivery task created from an external domain event.
 *
 * <p>The aggregate protects delivery lifecycle invariants. Provider-specific sending is handled
 * outside of the domain through sender ports.
 */
@Getter
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NullAssignment"})
public final class Notification {

  private final NotificationId id;
  private final SourceEventId sourceEventId;
  private final SourceEventType sourceEventType;
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
      SourceEventType sourceEventType,
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
    this.sourceEventType =
        Objects.requireNonNull(sourceEventType, "sourceEventType must not be null");
    this.type = Objects.requireNonNull(type, "type must not be null");
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.channel = Objects.requireNonNull(channel, "channel must not be null");
    this.destination = Objects.requireNonNull(destination, "destination must not be null");
    this.subject = Objects.requireNonNull(subject, "subject must not be null");
    this.body = Objects.requireNonNull(body, "body must not be null");
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.attempts = Objects.requireNonNull(attempts, "attempts must not be null");
    this.nextAttemptAt = nextAttemptAt;
    this.lastError = lastError;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    this.sentAt = sentAt;
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");

    validateState();
  }

  public static Notification pending(
      SourceEventId sourceEventId,
      SourceEventType sourceEventType,
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
      SourceEventType sourceEventType,
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
        null,
        new NotificationErrorMessage(reason),
        now,
        null,
        now);
  }

  public static Notification restore(
      NotificationId id,
      SourceEventId sourceEventId,
      SourceEventType sourceEventType,
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

  public void markSent() {
    requirePending("mark as sent");

    this.status = NotificationStatus.SENT;
    this.sentAt = Instant.now();
    this.nextAttemptAt = null;
    this.lastError = null;
    this.updatedAt = Instant.now();

    validateState();
  }

  public void markRetryableFailure(String errorMessage, Duration retryDelay) {
    requirePending("schedule retry");

    if (retryDelay == null || retryDelay.isNegative() || retryDelay.isZero()) {
      throw new NotificationDomainException("retry delay must be positive");
    }

    this.status = NotificationStatus.PENDING;
    this.attempts = attempts.increment();
    this.nextAttemptAt = Instant.now().plus(retryDelay);
    this.lastError = new NotificationErrorMessage(errorMessage);
    this.updatedAt = Instant.now();

    validateState();
  }

  public void markFailed(String errorMessage) {
    requirePending("mark as failed");

    this.status = NotificationStatus.FAILED;
    this.attempts = attempts.increment();
    this.nextAttemptAt = null;
    this.lastError = new NotificationErrorMessage(errorMessage);
    this.updatedAt = Instant.now();

    validateState();
  }

  public boolean isPending() {
    return status == NotificationStatus.PENDING;
  }

  public boolean reachedMaxAttempts(int maxAttempts) {
    if (maxAttempts <= 0) {
      throw new NotificationDomainException("max attempts must be positive");
    }

    return attempts.value() + 1 >= maxAttempts;
  }

  private void requirePending(String operation) {
    if (status != NotificationStatus.PENDING) {
      throw new NotificationDomainException(
          "Cannot " + operation + " notification with status " + status);
    }
  }

  private void validateState() {
    if (status == NotificationStatus.PENDING && nextAttemptAt == null) {
      throw new NotificationDomainException("pending notification must have nextAttemptAt");
    }

    if (status == NotificationStatus.SENT && sentAt == null) {
      throw new NotificationDomainException("sent notification must have sentAt");
    }

    if (status != NotificationStatus.SENT && sentAt != null) {
      throw new NotificationDomainException("only sent notification can have sentAt");
    }

    if ((status == NotificationStatus.FAILED || status == NotificationStatus.SKIPPED)
        && lastError == null) {
      throw new NotificationDomainException(status + " notification must have error message");
    }

    if ((status == NotificationStatus.SENT
            || status == NotificationStatus.FAILED
            || status == NotificationStatus.SKIPPED)
        && nextAttemptAt != null) {
      throw new NotificationDomainException(status + " notification must not have nextAttemptAt");
    }
  }
}
