package com.example.hotelbooking.notification.application.service;

import com.example.hotelbooking.notification.application.port.out.NotificationRepository;
import com.example.hotelbooking.notification.domain.Notification;
import com.example.hotelbooking.notification.domain.NotificationUserId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationHistoryService {

  private static final int MAX_LIMIT = 100;

  private final NotificationRepository notificationRepository;

  public List<Notification> findByUserId(UUID userId, int limit) {
    return notificationRepository.findByUserId(
        new NotificationUserId(userId), normalizeLimit(limit));
  }

  private int normalizeLimit(int limit) {
    if (limit <= 0) {
      return 10;
    }

    return Math.min(limit, MAX_LIMIT);
  }
}
