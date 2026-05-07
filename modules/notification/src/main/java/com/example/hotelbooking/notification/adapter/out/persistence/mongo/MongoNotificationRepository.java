package com.example.hotelbooking.notification.adapter.out.persistence.mongo;

import com.example.hotelbooking.notification.application.port.out.NotificationRepository;
import com.example.hotelbooking.notification.domain.Notification;
import com.example.hotelbooking.notification.domain.NotificationStatus;
import com.example.hotelbooking.notification.domain.NotificationUserId;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
@Profile("notification-mongo")
@RequiredArgsConstructor
class MongoNotificationRepository implements NotificationRepository {

  private final MongoTemplate mongoTemplate;
  private final SpringDataNotificationMongoRepository springDataRepository;

  @Override
  public boolean insertIfAbsent(Notification notification) {
    try {
      mongoTemplate.insert(NotificationDocument.from(notification));
      return true;
    } catch (DuplicateKeyException exception) {
      return false;
    }
  }

  @Override
  public Notification save(Notification notification) {
    return springDataRepository.save(NotificationDocument.from(notification)).toDomain();
  }

  @Override
  public List<Notification> findPendingForDelivery(Instant now, int limit) {
    Query query =
        Query.query(
                Criteria.where("status")
                    .is(NotificationStatus.PENDING.name())
                    .and("nextAttemptAt")
                    .lte(now))
            .with(Sort.by(Sort.Direction.ASC, "nextAttemptAt", "createdAt"))
            .limit(limit);

    return mongoTemplate.find(query, NotificationDocument.class).stream()
        .map(NotificationDocument::toDomain)
        .toList();
  }

  @Override
  public List<Notification> findByUserId(NotificationUserId userId) {
    return springDataRepository.findByUserIdOrderByCreatedAtDesc(userId.value().toString()).stream()
        .map(NotificationDocument::toDomain)
        .toList();
  }
}
