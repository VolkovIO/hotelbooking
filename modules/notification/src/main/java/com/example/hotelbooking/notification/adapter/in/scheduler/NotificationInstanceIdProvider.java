package com.example.hotelbooking.notification.adapter.in.scheduler;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class NotificationInstanceIdProvider {

  private final String nodeId = "notification-service-" + UUID.randomUUID();

  String instanceId() {
    return nodeId;
  }
}
