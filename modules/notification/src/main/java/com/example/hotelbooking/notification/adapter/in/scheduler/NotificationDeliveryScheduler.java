package com.example.hotelbooking.notification.adapter.in.scheduler;

import com.example.hotelbooking.notification.application.service.NotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.notification.delivery",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
class NotificationDeliveryScheduler {

  private final NotificationDeliveryService deliveryService;
  private final NotificationInstanceIdProvider instanceIdProvider;

  @Scheduled(fixedDelayString = "${app.notification.delivery.fixed-delay-ms:30000}")
  void deliverPendingNotifications() {
    int claimedCount = deliveryService.deliverPending(instanceIdProvider.instanceId());

    if (claimedCount > 0) {
      log.info("Processed claimed notifications: count={}", claimedCount);
    }
  }
}
