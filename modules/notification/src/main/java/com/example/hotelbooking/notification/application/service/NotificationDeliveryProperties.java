package com.example.hotelbooking.notification.application.service;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.notification.delivery")
public class NotificationDeliveryProperties {

  private boolean enabled = true;
  private int batchSize = 20;
  private int maxAttempts = 3;
  private Duration retryDelay = Duration.ofSeconds(30);
  private Duration lockDuration = Duration.ofMinutes(1);
}
