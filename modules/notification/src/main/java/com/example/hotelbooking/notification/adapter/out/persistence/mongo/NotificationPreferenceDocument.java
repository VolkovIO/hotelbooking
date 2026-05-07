package com.example.hotelbooking.notification.adapter.out.persistence.mongo;

import com.example.hotelbooking.notification.domain.NotificationChannel;
import com.example.hotelbooking.notification.domain.NotificationDestination;
import com.example.hotelbooking.notification.domain.NotificationPreference;
import com.example.hotelbooking.notification.domain.NotificationUserId;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document("notificationPreferences")
class NotificationPreferenceDocument {

  @Id private String userId;

  private String channel;
  private String destination;
  private boolean enabled;
  private Instant createdAt;
  private Instant updatedAt;

  static NotificationPreferenceDocument from(NotificationPreference preference) {
    return new NotificationPreferenceDocument(
        preference.getUserId().value().toString(),
        preference.getChannel().name(),
        preference.getDestination().value(),
        preference.isEnabled(),
        preference.getCreatedAt(),
        preference.getUpdatedAt());
  }

  NotificationPreference toDomain() {
    return NotificationPreference.restore(
        new NotificationUserId(UUID.fromString(userId)),
        NotificationChannel.valueOf(channel),
        new NotificationDestination(destination),
        enabled,
        createdAt,
        updatedAt);
  }
}
