package com.example.hotelbooking.notification.application.sender;

import com.example.hotelbooking.notification.domain.NotificationChannel;
import java.io.Serial;

public class NotificationSenderNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public NotificationSenderNotFoundException(NotificationChannel channel) {
    super("Notification sender is not configured for channel: " + channel);
  }
}
