package com.example.hotelbooking.notification.adapter.in.web.preference;

import com.example.hotelbooking.notification.application.service.NotificationPreferenceService;
import com.example.hotelbooking.notification.domain.NotificationPreference;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification-preferences")
@RequiredArgsConstructor
class NotificationPreferenceController {

  private final NotificationPreferenceService preferenceService;

  @PutMapping("/{userId}")
  ResponseEntity<NotificationPreferenceResponse> savePreference(
      @PathVariable UUID userId, @Valid @RequestBody UpdateNotificationPreferenceRequest request) {
    NotificationPreference preference =
        preferenceService.savePreference(
            userId, request.channel(), request.destination(), request.enabled());

    return ResponseEntity.ok(NotificationPreferenceResponse.from(preference));
  }

  @GetMapping("/{userId}")
  ResponseEntity<NotificationPreferenceResponse> findPreference(@PathVariable UUID userId) {
    return preferenceService
        .findByUserId(userId)
        .map(NotificationPreferenceResponse::from)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
