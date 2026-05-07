package com.example.hotelbooking.notification.application.port.out;

import com.example.hotelbooking.notification.domain.Notification;
import com.example.hotelbooking.notification.domain.NotificationUserId;
import java.time.Instant;
import java.util.List;

public interface NotificationRepository {

  boolean insertIfAbsent(Notification notification);

  Notification save(Notification notification);

  List<Notification> claimPendingForDelivery(
      String lockedBy, Instant now, Instant lockedUntil, int limit);

  List<Notification> findByUserId(NotificationUserId userId);
}
