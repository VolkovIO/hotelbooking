package com.example.hotelbooking.notification.adapter.out.persistence.mongo;

import com.example.hotelbooking.notification.domain.Notification;
import com.example.hotelbooking.notification.domain.NotificationAttemptCount;
import com.example.hotelbooking.notification.domain.NotificationBody;
import com.example.hotelbooking.notification.domain.NotificationChannel;
import com.example.hotelbooking.notification.domain.NotificationDestination;
import com.example.hotelbooking.notification.domain.NotificationErrorMessage;
import com.example.hotelbooking.notification.domain.NotificationId;
import com.example.hotelbooking.notification.domain.NotificationStatus;
import com.example.hotelbooking.notification.domain.NotificationSubject;
import com.example.hotelbooking.notification.domain.NotificationType;
import com.example.hotelbooking.notification.domain.NotificationUserId;
import com.example.hotelbooking.notification.domain.SourceAggregateId;
import com.example.hotelbooking.notification.domain.SourceCorrelationId;
import com.example.hotelbooking.notification.domain.SourceEventId;
import com.example.hotelbooking.notification.domain.SourceEventType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document("notifications")
@CompoundIndex(
    name = "ux_notifications_source_event_type",
    def = "{'sourceEventId': 1, 'type': 1}",
    unique = true)
@CompoundIndex(
    name = "ix_notifications_status_next_attempt",
    def = "{'status': 1, 'nextAttemptAt': 1}")
@CompoundIndex(
    name = "ix_notifications_delivery_claim",
    def = "{'status': 1, 'nextAttemptAt': 1, 'lockedUntil': 1}")
class NotificationDocument {

  @Id private String id;

  private String sourceEventId;
  private String sourceEventType;
  private String sourceAggregateId;
  private String sourceCorrelationId;
  private String type;
  private String userId;
  private String channel;
  private String destination;
  private String subject;
  private String body;
  private String status;
  private int attempts;
  private Instant nextAttemptAt;
  private String lastError;
  private Instant createdAt;
  private Instant sentAt;
  private Instant updatedAt;
  private String lockedBy;
  private Instant lockedUntil;

  static NotificationDocument from(Notification notification) {
    return new NotificationDocument(
        notification.getId().value().toString(),
        notification.getSourceEventId().value().toString(),
        notification.getSourceEventType().value(),
        notification.getSourceAggregateId().value().toString(),
        notification.getSourceCorrelationId().value().toString(),
        notification.getType().name(),
        notification.getUserId().value().toString(),
        notification.getChannel().name(),
        notification.getDestination().value(),
        notification.getSubject().value(),
        notification.getBody().value(),
        notification.getStatus().name(),
        notification.getAttempts().value(),
        notification.getNextAttemptAt(),
        lastErrorValue(notification),
        notification.getCreatedAt(),
        notification.getSentAt(),
        notification.getUpdatedAt(),
        null,
        null);
  }

  Notification toDomain() {
    UUID eventId = UUID.fromString(sourceEventId);

    return Notification.restore(
        new NotificationId(UUID.fromString(id)),
        new SourceEventId(eventId),
        new SourceEventType(sourceEventType),
        new SourceAggregateId(uuidOrFallback(sourceAggregateId, eventId)),
        new SourceCorrelationId(uuidOrFallback(sourceCorrelationId, eventId)),
        NotificationType.valueOf(type),
        new NotificationUserId(UUID.fromString(userId)),
        NotificationChannel.valueOf(channel),
        new NotificationDestination(destination),
        new NotificationSubject(subject),
        new NotificationBody(body),
        NotificationStatus.valueOf(status),
        new NotificationAttemptCount(attempts),
        nextAttemptAt,
        toErrorMessage(lastError),
        createdAt,
        sentAt,
        updatedAt);
  }

  private static UUID uuidOrFallback(String value, UUID fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }

    return UUID.fromString(value);
  }

  private static String lastErrorValue(Notification notification) {
    if (notification.getLastError() == null) {
      return null;
    }

    return notification.getLastError().value();
  }

  private static NotificationErrorMessage toErrorMessage(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return new NotificationErrorMessage(value);
  }
}
