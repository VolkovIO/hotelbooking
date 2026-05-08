package com.example.hotelbooking.notification.application.service;

import com.example.hotelbooking.notification.application.port.out.NotificationRepository;
import com.example.hotelbooking.notification.application.port.out.NotificationSender;
import com.example.hotelbooking.notification.application.sender.NotificationMessage;
import com.example.hotelbooking.notification.application.sender.NotificationSenderNotFoundException;
import com.example.hotelbooking.notification.application.sender.NotificationSenderRegistry;
import com.example.hotelbooking.notification.application.sender.SendNotificationResult;
import com.example.hotelbooking.notification.domain.Notification;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

  private static final String GENERIC_SENDER_FAILURE = "notification sender returned failure";

  private final NotificationRepository notificationRepository;
  private final NotificationSenderRegistry senderRegistry;
  private final NotificationDeliveryProperties properties;

  public int deliverPending(String lockedBy) {
    Instant now = Instant.now();
    Instant lockedUntil = now.plus(properties.getLockDuration());

    List<Notification> notifications =
        notificationRepository.claimPendingForDelivery(
            lockedBy, now, lockedUntil, properties.getBatchSize());

    notifications.forEach(notification -> deliverOne(notification, lockedBy));

    return notifications.size();
  }

  private void deliverOne(Notification notification, String lockedBy) {
    try {
      NotificationSender sender = senderRegistry.getSender(notification.getChannel());
      SendNotificationResult result = sender.send(NotificationMessage.from(notification));
      applySendResult(notification, result);
    } catch (NotificationSenderNotFoundException exception) {
      applyFailure(notification, exception.getMessage());
    }

    notificationRepository.saveClaimed(notification, lockedBy);
  }

  private void applySendResult(Notification notification, SendNotificationResult result) {
    if (result.success()) {
      notification.markSent();
      return;
    }

    applyFailure(notification, failureMessage(result));
  }

  private void applyFailure(Notification notification, String errorMessage) {
    if (notification.reachedMaxAttempts(properties.getMaxAttempts())) {
      notification.markFailed(errorMessage);
      return;
    }

    notification.markRetryableFailure(errorMessage, properties.getRetryDelay());
  }

  private String failureMessage(SendNotificationResult result) {
    if (result.errorMessage() == null || result.errorMessage().isBlank()) {
      return GENERIC_SENDER_FAILURE;
    }

    return result.errorMessage();
  }
}
