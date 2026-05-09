package com.example.hotelbooking.booking.application.saga;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.booking.saga.retry")
public class BookingSagaRetryProperties {

  private boolean enabled = true;
  private int maxRetries = 3;
  private int batchSize = 20;
  private Duration delay = Duration.ofSeconds(10);
  private long schedulerFixedDelayMs = 5_000L;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public Duration getDelay() {
    return delay;
  }

  public void setDelay(Duration delay) {
    this.delay = delay;
  }

  public long getSchedulerFixedDelayMs() {
    return schedulerFixedDelayMs;
  }

  public void setSchedulerFixedDelayMs(long schedulerFixedDelayMs) {
    this.schedulerFixedDelayMs = schedulerFixedDelayMs;
  }
}
