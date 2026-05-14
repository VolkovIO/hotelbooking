package com.example.hotelbooking.notification.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTest {

  private static final String SOURCE_EVENT_TYPE = "BookingConfirmed";
  private static final String SUBJECT = "Booking confirmed";
  private static final String BODY = "Your booking has been confirmed.";
  private static final String DESTINATION = "user@example.com";

  @Test
  void shouldCreatePendingNotification() {
    Notification notification = pendingNotification();

    assertEquals(NotificationStatus.PENDING, notification.getStatus());
    assertEquals(NotificationAttemptCount.zero(), notification.getAttempts());
    assertNotNull(notification.getNextAttemptAt());
    assertNull(notification.getSentAt());
    assertNull(notification.getLastError());
    assertTrue(notification.isPending());
    assertNotNull(notification.getSourceAggregateId());
    assertNotNull(notification.getSourceCorrelationId());
  }

  @Test
  void shouldMarkPendingNotificationAsSent() {
    Notification notification = pendingNotification();

    notification.markSent();

    assertEquals(NotificationStatus.SENT, notification.getStatus());
    assertNotNull(notification.getSentAt());
    assertNull(notification.getNextAttemptAt());
    assertNull(notification.getLastError());
    assertFalse(notification.isPending());
  }

  @Test
  void shouldScheduleRetryForPendingNotification() {
    Notification notification = pendingNotification();

    notification.markRetryableFailure("temporary failure", Duration.ofSeconds(30));

    assertEquals(NotificationStatus.PENDING, notification.getStatus());
    assertEquals(new NotificationAttemptCount(1), notification.getAttempts());
    assertNotNull(notification.getNextAttemptAt());
    assertEquals(new NotificationErrorMessage("temporary failure"), notification.getLastError());
  }

  @Test
  void shouldMarkPendingNotificationAsFailed() {
    Notification notification = pendingNotification();

    notification.markFailed("permanent failure");

    assertEquals(NotificationStatus.FAILED, notification.getStatus());
    assertEquals(new NotificationAttemptCount(1), notification.getAttempts());
    assertNull(notification.getNextAttemptAt());
    assertEquals(new NotificationErrorMessage("permanent failure"), notification.getLastError());
  }

  @Test
  void shouldCreateSkippedNotificationWithReason() {
    Notification notification =
        Notification.skipped(
            new SourceEventId(UUID.randomUUID()),
            new SourceEventType(SOURCE_EVENT_TYPE),
            new SourceAggregateId(UUID.randomUUID()),
            new SourceCorrelationId(UUID.randomUUID()),
            NotificationType.BOOKING_CONFIRMED,
            new NotificationUserId(UUID.randomUUID()),
            new NotificationSubject(SUBJECT),
            new NotificationBody("Notification preference was not found."),
            "notification preference not found");

    assertEquals(NotificationStatus.SKIPPED, notification.getStatus());
    assertEquals(
        new NotificationErrorMessage("notification preference not found"),
        notification.getLastError());
    assertNull(notification.getNextAttemptAt());
    assertNull(notification.getSentAt());
  }

  @Test
  void shouldRejectSentNotificationWithoutSentAtOnRestore() {
    Instant now = Instant.now();

    assertThrows(
        NotificationDomainException.class,
        () ->
            Notification.restore(
                NotificationId.newId(),
                new SourceEventId(UUID.randomUUID()),
                new SourceEventType(SOURCE_EVENT_TYPE),
                new SourceAggregateId(UUID.randomUUID()),
                new SourceCorrelationId(UUID.randomUUID()),
                NotificationType.BOOKING_CONFIRMED,
                new NotificationUserId(UUID.randomUUID()),
                NotificationChannel.EMAIL,
                new NotificationDestination(DESTINATION),
                new NotificationSubject(SUBJECT),
                new NotificationBody(BODY),
                NotificationStatus.SENT,
                NotificationAttemptCount.zero(),
                null,
                null,
                now,
                null,
                now));
  }

  @Test
  void shouldRejectFailedNotificationWithoutErrorOnRestore() {
    Instant now = Instant.now();

    assertThrows(
        NotificationDomainException.class,
        () ->
            Notification.restore(
                NotificationId.newId(),
                new SourceEventId(UUID.randomUUID()),
                new SourceEventType(SOURCE_EVENT_TYPE),
                new SourceAggregateId(UUID.randomUUID()),
                new SourceCorrelationId(UUID.randomUUID()),
                NotificationType.BOOKING_CONFIRMED,
                new NotificationUserId(UUID.randomUUID()),
                NotificationChannel.EMAIL,
                new NotificationDestination(DESTINATION),
                new NotificationSubject(SUBJECT),
                new NotificationBody(BODY),
                NotificationStatus.FAILED,
                new NotificationAttemptCount(1),
                null,
                null,
                now,
                null,
                now));
  }

  @Test
  void shouldRejectRetryForSentNotification() {
    Notification notification = pendingNotification();

    notification.markSent();

    assertThrows(
        NotificationDomainException.class,
        () -> notification.markRetryableFailure("failure", Duration.ofSeconds(30)));
  }

  @Test
  void shouldRejectFailureForSentNotification() {
    Notification notification = pendingNotification();

    notification.markSent();

    assertThrows(
        NotificationDomainException.class, () -> notification.markFailed("permanent failure"));
  }

  @Test
  void shouldRejectNonPositiveRetryDelay() {
    Notification notification = pendingNotification();

    assertThrows(
        NotificationDomainException.class,
        () -> notification.markRetryableFailure("failure", Duration.ZERO));
  }

  @Test
  void shouldCheckMaxAttempts() {
    Notification notification = pendingNotification();

    assertTrue(notification.reachedMaxAttempts(1));
    assertFalse(notification.reachedMaxAttempts(3));
  }

  private Notification pendingNotification() {
    return Notification.pending(
        new SourceEventId(UUID.randomUUID()),
        new SourceEventType(SOURCE_EVENT_TYPE),
        new SourceAggregateId(UUID.randomUUID()),
        new SourceCorrelationId(UUID.randomUUID()),
        NotificationType.BOOKING_CONFIRMED,
        new NotificationUserId(UUID.randomUUID()),
        NotificationChannel.EMAIL,
        new NotificationDestination(DESTINATION),
        new NotificationSubject(SUBJECT),
        new NotificationBody(BODY));
  }
}
