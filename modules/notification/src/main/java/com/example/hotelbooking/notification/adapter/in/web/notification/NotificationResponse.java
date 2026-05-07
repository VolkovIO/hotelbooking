package com.example.hotelbooking.notification.adapter.in.web.notification;

import com.example.hotelbooking.notification.domain.Notification;
import java.time.Instant;
import java.util.UUID;

record NotificationResponse(
    UUID id,
    UUID sourceEventId,
    String sourceEventType,
    String type,
    UUID userId,
    String channel,
    String destination,
    String subject,
    String body,
    String status,
    int attempts,
    Instant nextAttemptAt,
    String lastError,
    Instant createdAt,
    Instant sentAt,
    Instant updatedAt) {

  static NotificationResponse from(Notification notification) {
    return new NotificationResponse(
        notification.getId().value(),
        notification.getSourceEventId().value(),
        notification.getSourceEventType().value(),
        notification.getType().name(),
        notification.getUserId().value(),
        notification.getChannel().name(),
        notification.getDestination().value(),
        notification.getSubject().value(),
        notification.getBody().value(),
        notification.getStatus().name(),
        notification.getAttempts().value(),
        notification.getNextAttemptAt(),
        lastError(notification),
        notification.getCreatedAt(),
        notification.getSentAt(),
        notification.getUpdatedAt());
  }

  private static String lastError(Notification notification) {
    if (notification.getLastError() == null) {
      return null;
    }

    return notification.getLastError().value();
  }
}
