package com.example.hotelbooking.notification.application.service;

import com.example.hotelbooking.notification.application.port.out.NotificationMetrics;
import com.example.hotelbooking.notification.application.port.out.NotificationObservabilityContext;
import com.example.hotelbooking.notification.application.port.out.NotificationRepository;
import com.example.hotelbooking.notification.application.port.out.NotificationSender;
import com.example.hotelbooking.notification.application.sender.NotificationMessage;
import com.example.hotelbooking.notification.application.sender.NotificationSenderNotFoundException;
import com.example.hotelbooking.notification.application.sender.NotificationSenderRegistry;
import com.example.hotelbooking.notification.application.sender.SendNotificationResult;
import com.example.hotelbooking.notification.domain.Notification;
import com.example.hotelbooking.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

  private static final String GENERIC_SENDER_FAILURE = "notification sender returned failure";
  private static final String OUTCOME_SENT = "sent";
  private static final String OUTCOME_RETRY_SCHEDULED = "retry_scheduled";
  private static final String OUTCOME_FAILED = "failed";

  private final NotificationRepository notificationRepository;
  private final NotificationSenderRegistry senderRegistry;
  private final NotificationDeliveryProperties properties;
  private final NotificationObservabilityContext observabilityContext;
  private final NotificationMetrics notificationMetrics;

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
    try (NotificationObservabilityContext.ContextScope ignored =
        observabilityContext.openNotificationDelivery(notification)) {
      try {
        NotificationSender sender = senderRegistry.getSender(notification.getChannel());
        SendNotificationResult result = sender.send(NotificationMessage.from(notification));
        applySendResult(notification, result);
      } catch (NotificationSenderNotFoundException exception) {
        applyFailure(notification, exception.getMessage());
      }

      notificationRepository.saveClaimed(notification, lockedBy);

      notificationMetrics.deliveryProcessed(channel(notification), deliveryOutcome(notification));
    }
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

  private String deliveryOutcome(Notification notification) {
    if (notification.getStatus() == NotificationStatus.SENT) {
      return OUTCOME_SENT;
    }

    if (notification.getStatus() == NotificationStatus.FAILED) {
      return OUTCOME_FAILED;
    }

    return OUTCOME_RETRY_SCHEDULED;
  }

  private String channel(Notification notification) {
    return notification.getChannel().name().toLowerCase(Locale.ROOT);
  }
}
