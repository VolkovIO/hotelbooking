package com.example.hotelbooking.payment.application.outbox;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@Profile("payment-outbox-publisher")
@ConfigurationProperties(prefix = "app.payment.outbox.polling")
public class PaymentOutboxPollingProperties {

  private boolean enabled = true;
  private int batchSize = 20;
  private int maxAttempts = 3;
  private Duration retryDelay = Duration.ofSeconds(30);
}
