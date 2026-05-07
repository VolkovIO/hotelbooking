package com.example.hotelbooking.notification.adapter.in.web.notification;

import com.example.hotelbooking.notification.application.service.NotificationHistoryService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
class NotificationHistoryController {

  private final NotificationHistoryService historyService;

  @GetMapping
  List<NotificationResponse> findByUserId(
      @RequestParam UUID userId, @RequestParam(defaultValue = "10") int limit) {
    return historyService.findByUserId(userId, limit).stream()
        .map(NotificationResponse::from)
        .toList();
  }
}
