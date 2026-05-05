package com.example.hotelbooking.bookingservice.outbox;

import com.example.hotelbooking.booking.application.service.BookingOutboxPollingService;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("outbox-publisher")
class BookingOutboxScheduler {

  private final BookingOutboxPollingService pollingService;
  private final int batchSize;
  private final int maxAttempts;
  private final Duration retryDelay;
  private final String lockedBy;

  BookingOutboxScheduler(
      BookingOutboxPollingService pollingService,
      @Value("${app.booking.outbox.publisher.batch-size:10}") int batchSize,
      @Value("${app.booking.outbox.publisher.max-attempts:3}") int maxAttempts,
      @Value("${app.booking.outbox.publisher.retry-delay:PT30S}") Duration retryDelay,
      @Value("${spring.application.name:booking-service}") String applicationName) {
    this.pollingService = pollingService;
    this.batchSize = batchSize;
    this.maxAttempts = maxAttempts;
    this.retryDelay = retryDelay;
    this.lockedBy = applicationName + "-" + hostname();
  }

  @Scheduled(fixedDelayString = "${app.booking.outbox.publisher.fixed-delay-ms:5000}")
  void publishAvailableOutboxMessages() {
    int publishedCount =
        pollingService.publishAvailableMessages(batchSize, maxAttempts, retryDelay, lockedBy);

    if (publishedCount > 0) {
      log.info("Booking outbox scheduler processed messages: count={}", publishedCount);
    }
  }

  private String hostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException exception) {
      return "unknown-host";
    }
  }
}
