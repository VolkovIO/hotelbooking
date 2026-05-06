package com.example.hotelbooking.notification.adapter.in.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class NotificationHealthController {

  @GetMapping("/api/v1/notifications/health")
  Map<String, String> health() {
    return Map.of("status", "OK", "service", "notification-service");
  }
}
