package com.example.hotelbooking.notification.application.sender;

import com.example.hotelbooking.notification.application.port.out.NotificationSender;
import com.example.hotelbooking.notification.domain.NotificationChannel;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationSenderRegistry {

  private final Map<NotificationChannel, NotificationSender> senders;

  public NotificationSenderRegistry(List<NotificationSender> senders) {
    this.senders = new EnumMap<>(NotificationChannel.class);

    for (NotificationSender sender : senders) {
      this.senders.put(sender.channel(), sender);
    }
  }

  public NotificationSender getSender(NotificationChannel channel) {
    NotificationSender sender = senders.get(channel);

    if (sender == null) {
      throw new NotificationSenderNotFoundException(channel);
    }

    return sender;
  }
}
