package com.example.hotelbooking.notification.adapter.out.persistence.mongo;

import com.example.hotelbooking.notification.application.port.out.NotificationRepository;
import com.example.hotelbooking.notification.domain.Notification;
import com.example.hotelbooking.notification.domain.NotificationStatus;
import com.example.hotelbooking.notification.domain.NotificationUserId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@Profile("notification-mongo")
@RequiredArgsConstructor
class MongoNotificationRepository implements NotificationRepository {

  private static final String FIELD_STATUS = "status";
  private static final String FIELD_NEXT_ATTEMPT_AT = "nextAttemptAt";
  private static final String FIELD_CREATED_AT = "createdAt";
  private static final String FIELD_UPDATED_AT = "updatedAt";
  private static final String FIELD_LOCKED_BY = "lockedBy";
  private static final String FIELD_LOCKED_UNTIL = "lockedUntil";

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
  public List<Notification> claimPendingForDelivery(
      String lockedBy, Instant now, Instant lockedUntil, int limit) {
    validateClaimArguments(lockedBy, now, lockedUntil, limit);

    List<Notification> claimedNotifications = new ArrayList<>();

    for (int index = 0; index < limit; index++) {
      NotificationDocument document = claimOne(lockedBy, now, lockedUntil);

      if (document == null) {
        break;
      }

      claimedNotifications.add(document.toDomain());
    }

    return claimedNotifications;
  }

  @Override
  public List<Notification> findByUserId(NotificationUserId userId) {
    return springDataRepository.findByUserIdOrderByCreatedAtDesc(userId.value().toString()).stream()
        .map(NotificationDocument::toDomain)
        .toList();
  }

  private NotificationDocument claimOne(String lockedBy, Instant now, Instant lockedUntil) {
    Query query =
        Query.query(
                Criteria.where(FIELD_STATUS)
                    .is(NotificationStatus.PENDING.name())
                    .and(FIELD_NEXT_ATTEMPT_AT)
                    .lte(now))
            .addCriteria(lockAvailableCriteria(now))
            .with(Sort.by(Sort.Direction.ASC, FIELD_NEXT_ATTEMPT_AT, FIELD_CREATED_AT));

    Update update =
        new Update()
            .set(FIELD_LOCKED_BY, lockedBy)
            .set(FIELD_LOCKED_UNTIL, lockedUntil)
            .set(FIELD_UPDATED_AT, now);

    return mongoTemplate.findAndModify(
        query, update, FindAndModifyOptions.options().returnNew(true), NotificationDocument.class);
  }

  private Criteria lockAvailableCriteria(Instant now) {
    return new Criteria()
        .orOperator(
            Criteria.where(FIELD_LOCKED_UNTIL).exists(false),
            Criteria.where(FIELD_LOCKED_UNTIL).lte(now));
  }

  private void validateClaimArguments(
      String lockedBy, Instant now, Instant lockedUntil, int limit) {
    if (lockedBy == null || lockedBy.isBlank()) {
      throw new IllegalArgumentException("lockedBy must not be blank");
    }

    Objects.requireNonNull(now, "now must not be null");
    Objects.requireNonNull(lockedUntil, "lockedUntil must not be null");

    if (!lockedUntil.isAfter(now)) {
      throw new IllegalArgumentException("lockedUntil must be after now");
    }

    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be positive");
    }
  }
}
