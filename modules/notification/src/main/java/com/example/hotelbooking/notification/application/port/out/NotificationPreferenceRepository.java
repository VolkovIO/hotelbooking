package com.example.hotelbooking.notification.application.port.out;

import com.example.hotelbooking.notification.domain.NotificationPreference;
import com.example.hotelbooking.notification.domain.NotificationUserId;
import java.util.Optional;

public interface NotificationPreferenceRepository {

  NotificationPreference save(NotificationPreference preference);

  Optional<NotificationPreference> findByUserId(NotificationUserId userId);
}
