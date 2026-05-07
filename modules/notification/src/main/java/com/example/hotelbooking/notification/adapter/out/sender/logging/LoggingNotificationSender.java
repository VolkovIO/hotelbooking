package com.example.hotelbooking.notification.adapter.out.sender.logging;

import com.example.hotelbooking.notification.application.port.out.NotificationSender;
import com.example.hotelbooking.notification.application.sender.NotificationMessage;
import com.example.hotelbooking.notification.application.sender.SendNotificationResult;
import com.example.hotelbooking.notification.domain.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
abstract class LoggingNotificationSender implements NotificationSender {

  private final NotificationChannel senderChannel;

  @Override
  public NotificationChannel channel() {
    return senderChannel;
  }

  @Override
  public SendNotificationResult send(NotificationMessage message) {
    log.info(
        "Sending notification through logging adapter: "
            + "channel={}, notificationId={}, userId={}, destination={}, subject={}, body={}",
        senderChannel,
        message.notificationId().value(),
        message.userId().value(),
        message.destination().value(),
        message.subject().value(),
        message.body().value());

    return SendNotificationResult.success("logging-" + message.notificationId().value());
  }
}
