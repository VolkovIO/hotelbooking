package com.example.hotelbooking.booking.application.service;

import com.example.hotelbooking.booking.application.event.BookingOutboxMessage;
import com.example.hotelbooking.booking.application.exception.BookingOutboxPublicationException;
import com.example.hotelbooking.booking.application.port.out.BookingOutboxEventPublisher;
import com.example.hotelbooking.booking.application.port.out.BookingOutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingOutboxPollingService {

  private static final int MAX_ERROR_MESSAGE_LENGTH = 2_000;

  private final BookingOutboxRepository bookingOutboxRepository;
  private final BookingOutboxEventPublisher bookingOutboxEventPublisher;

  public int publishAvailableMessages(
      int batchSize, int maxAttempts, Duration retryDelay, String lockedBy) {
    List<BookingOutboxMessage> messages =
        bookingOutboxRepository.claimBatchForProcessing(batchSize, Instant.now(), lockedBy);

    if (messages.isEmpty()) {
      log.debug("No booking outbox messages available for publishing");
      return 0;
    }

    log.info("Claimed booking outbox messages for publishing: count={}", messages.size());

    for (BookingOutboxMessage message : messages) {
      publishSingleMessage(message, maxAttempts, retryDelay);
    }

    return messages.size();
  }

  private void publishSingleMessage(
      BookingOutboxMessage message, int maxAttempts, Duration retryDelay) {
    try {
      bookingOutboxEventPublisher.publish(message);
      bookingOutboxRepository.markPublished(message.id(), Instant.now());

      log.info(
          "Booking outbox message published: eventId={}, eventType={}, aggregateId={}",
          message.id(),
          message.eventType(),
          message.aggregateId());
    } catch (BookingOutboxPublicationException exception) {
      handlePublishingFailure(message, maxAttempts, retryDelay, exception);
    }
  }

  private void handlePublishingFailure(
      BookingOutboxMessage message,
      int maxAttempts,
      Duration retryDelay,
      BookingOutboxPublicationException exception) {
    String errorMessage = limitErrorMessage(exception);
    int nextAttempt = message.attempts() + 1;

    if (nextAttempt >= maxAttempts) {
      bookingOutboxRepository.markTerminalFailure(message.id(), errorMessage);

      log.error(
          "Booking outbox message failed permanently: eventId={}, eventType={}, attempts={}",
          message.id(),
          message.eventType(),
          nextAttempt,
          exception);
      return;
    }

    bookingOutboxRepository.markRetryableFailure(
        message.id(), Instant.now().plus(retryDelay), errorMessage);

    log.warn(
        "Booking outbox message failed and will be retried: "
            + "eventId={}, eventType={}, nextAttempt={}, maxAttempts={}, retryDelay={}",
        message.id(),
        message.eventType(),
        nextAttempt,
        maxAttempts,
        retryDelay,
        exception);
  }

  private String limitErrorMessage(BookingOutboxPublicationException exception) {
    String message = exception.getMessage();

    if (message == null || message.isBlank()) {
      message = exception.getClass().getName();
    }

    if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
      return message;
    }

    return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
  }
}
