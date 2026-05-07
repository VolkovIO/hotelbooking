package com.example.hotelbooking.notification.adapter.out.persistence.mongo;

import com.example.hotelbooking.notification.application.port.out.NotificationPreferenceRepository;
import com.example.hotelbooking.notification.domain.NotificationPreference;
import com.example.hotelbooking.notification.domain.NotificationUserId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("notification-mongo")
@RequiredArgsConstructor
class MongoNotificationPreferenceRepository implements NotificationPreferenceRepository {

  private final SpringDataNotificationPreferenceMongoRepository springDataRepository;

  @Override
  public NotificationPreference save(NotificationPreference preference) {
    return springDataRepository.save(NotificationPreferenceDocument.from(preference)).toDomain();
  }

  @Override
  public Optional<NotificationPreference> findByUserId(NotificationUserId userId) {
    return springDataRepository
        .findById(userId.value().toString())
        .map(NotificationPreferenceDocument::toDomain);
  }
}
