package com.example.hotelbooking.booking.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hotelbooking.booking.application.event.BookingOutboxMessage;
import com.example.hotelbooking.booking.application.exception.BookingOutboxPublicationException;
import com.example.hotelbooking.booking.application.port.out.BookingOutboxEventPublisher;
import com.example.hotelbooking.booking.application.port.out.BookingOutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingOutboxPollingServiceTest {

  private static final int BATCH_SIZE = 10;
  private static final int MAX_ATTEMPTS = 3;
  private static final Duration RETRY_DELAY = Duration.ofSeconds(30);
  private static final String LOCKED_BY = "test-node";

  @Mock private BookingOutboxRepository repository;

  @Mock private BookingOutboxEventPublisher publisher;

  @InjectMocks private BookingOutboxPollingService service;

  @Test
  void shouldPublishClaimedMessagesAndMarkThemPublished() throws BookingOutboxPublicationException {
    BookingOutboxMessage message = message(0);

    when(repository.claimBatchForProcessing(eq(BATCH_SIZE), any(), eq(LOCKED_BY)))
        .thenReturn(List.of(message));

    service.publishAvailableMessages(BATCH_SIZE, MAX_ATTEMPTS, RETRY_DELAY, LOCKED_BY);

    verify(publisher).publish(message);
    verify(repository).markPublished(eq(message.id()), any());
  }

  @Test
  void shouldMarkMessageForRetryWhenPublishingFails() throws BookingOutboxPublicationException {
    BookingOutboxMessage message = message(0);

    when(repository.claimBatchForProcessing(eq(BATCH_SIZE), any(), eq(LOCKED_BY)))
        .thenReturn(List.of(message));

    doThrow(new BookingOutboxPublicationException("temporary failure"))
        .when(publisher)
        .publish(message);

    service.publishAvailableMessages(BATCH_SIZE, MAX_ATTEMPTS, RETRY_DELAY, LOCKED_BY);

    verify(repository).markRetryableFailure(eq(message.id()), any(), contains("temporary failure"));
    verify(repository, never()).markPublished(any(), any());
  }

  @Test
  void shouldMarkMessageAsFailedWhenMaxAttemptsReached() throws BookingOutboxPublicationException {
    BookingOutboxMessage message = message(2);

    when(repository.claimBatchForProcessing(eq(BATCH_SIZE), any(), eq(LOCKED_BY)))
        .thenReturn(List.of(message));

    doThrow(new BookingOutboxPublicationException("permanent failure"))
        .when(publisher)
        .publish(message);

    service.publishAvailableMessages(BATCH_SIZE, MAX_ATTEMPTS, RETRY_DELAY, LOCKED_BY);

    verify(repository).markTerminalFailure(eq(message.id()), contains("permanent failure"));
  }

  private BookingOutboxMessage message(int attempts) {
    return new BookingOutboxMessage(
        UUID.randomUUID(),
        "Booking",
        UUID.randomUUID(),
        "BookingConfirmed",
        1,
        Map.of("bookingId", UUID.randomUUID().toString()),
        Instant.now(),
        attempts,
        UUID.randomUUID(),
        null);
  }
}
