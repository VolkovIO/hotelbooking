package com.example.hotelbooking.notification.application.sender;

import com.example.hotelbooking.notification.domain.Notification;
import com.example.hotelbooking.notification.domain.NotificationBody;
import com.example.hotelbooking.notification.domain.NotificationChannel;
import com.example.hotelbooking.notification.domain.NotificationDestination;
import com.example.hotelbooking.notification.domain.NotificationId;
import com.example.hotelbooking.notification.domain.NotificationSubject;
import com.example.hotelbooking.notification.domain.NotificationUserId;
import java.util.Objects;

public record NotificationMessage(
    NotificationId notificationId,
    NotificationUserId userId,
    NotificationChannel channel,
    NotificationDestination destination,
    NotificationSubject subject,
    NotificationBody body) {

  public NotificationMessage {
    Objects.requireNonNull(notificationId, "notificationId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(channel, "channel must not be null");
    Objects.requireNonNull(destination, "destination must not be null");
    Objects.requireNonNull(subject, "subject must not be null");
    Objects.requireNonNull(body, "body must not be null");
  }

  public static NotificationMessage from(Notification notification) {
    return new NotificationMessage(
        notification.getId(),
        notification.getUserId(),
        notification.getChannel(),
        notification.getDestination(),
        notification.getSubject(),
        notification.getBody());
  }
}
