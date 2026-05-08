package com.example.hotelbooking.notification.application.port.out;

import com.example.hotelbooking.notification.application.sender.NotificationMessage;
import com.example.hotelbooking.notification.application.sender.SendNotificationResult;
import com.example.hotelbooking.notification.domain.NotificationChannel;

public interface NotificationSender {

  NotificationChannel channel();

  SendNotificationResult send(NotificationMessage message);
}
