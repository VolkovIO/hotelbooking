package com.example.hotelbooking.notification.adapter.out.sender.logging;

import com.example.hotelbooking.notification.domain.NotificationChannel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("notification-senders-logging")
class LoggingTelegramNotificationSender extends LoggingNotificationSender {

  LoggingTelegramNotificationSender() {
    super(NotificationChannel.TELEGRAM);
  }
}
