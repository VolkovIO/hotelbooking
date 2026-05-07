package com.example.hotelbooking.notification.adapter.in.web.preference;

import com.example.hotelbooking.notification.domain.NotificationPreference;
import java.time.Instant;
import java.util.UUID;

record NotificationPreferenceResponse(
    UUID userId,
    String channel,
    String destination,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt) {

  static NotificationPreferenceResponse from(NotificationPreference preference) {
    return new NotificationPreferenceResponse(
        preference.getUserId().value(),
        preference.getChannel().name(),
        preference.getDestination().value(),
        preference.isEnabled(),
        preference.getCreatedAt(),
        preference.getUpdatedAt());
  }
}
