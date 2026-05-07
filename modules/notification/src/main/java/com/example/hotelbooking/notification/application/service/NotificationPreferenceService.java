package com.example.hotelbooking.notification.application.service;

import com.example.hotelbooking.notification.application.port.out.NotificationPreferenceRepository;
import com.example.hotelbooking.notification.domain.NotificationChannel;
import com.example.hotelbooking.notification.domain.NotificationDestination;
import com.example.hotelbooking.notification.domain.NotificationPreference;
import com.example.hotelbooking.notification.domain.NotificationUserId;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

  private final NotificationPreferenceRepository preferenceRepository;

  public NotificationPreference savePreference(
      UUID userId, NotificationChannel channel, String destination, boolean enabled) {
    NotificationUserId notificationUserId = new NotificationUserId(userId);
    NotificationDestination notificationDestination = new NotificationDestination(destination);

    Optional<NotificationPreference> existingPreference =
        preferenceRepository.findByUserId(notificationUserId);

    if (existingPreference.isPresent()) {
      NotificationPreference preference = existingPreference.get();
      preference.update(channel, notificationDestination, enabled);
      return preferenceRepository.save(preference);
    }

    NotificationPreference preference =
        NotificationPreference.create(
            notificationUserId, channel, notificationDestination, enabled);

    return preferenceRepository.save(preference);
  }

  public Optional<NotificationPreference> findByUserId(UUID userId) {
    return preferenceRepository.findByUserId(new NotificationUserId(userId));
  }
}
