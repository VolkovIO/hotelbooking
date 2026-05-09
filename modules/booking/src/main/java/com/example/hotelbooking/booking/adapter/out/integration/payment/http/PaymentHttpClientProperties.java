package com.example.hotelbooking.booking.adapter.out.integration.payment.http;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.booking.payment-client")
public record PaymentHttpClientProperties(String baseUrl, Duration responseTimeout) {

  private static final String DEFAULT_BASE_URL = "http://localhost:8083";
  private static final Duration DEFAULT_RESPONSE_TIMEOUT = Duration.ofSeconds(5);

  public PaymentHttpClientProperties {
    if (baseUrl == null || baseUrl.isBlank()) {
      baseUrl = DEFAULT_BASE_URL;
    }

    if (responseTimeout == null) {
      responseTimeout = DEFAULT_RESPONSE_TIMEOUT;
    }
  }
}
