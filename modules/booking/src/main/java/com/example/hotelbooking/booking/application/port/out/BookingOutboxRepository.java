package com.example.hotelbooking.booking.application.port.out;

import com.example.hotelbooking.booking.application.event.BookingOutboxMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BookingOutboxRepository {

  void save(BookingOutboxMessage message);

  List<BookingOutboxMessage> claimBatchForProcessing(int batchSize, Instant now, String lockedBy);

  void markPublished(UUID messageId, Instant publishedAt);

  void markRetryableFailure(UUID messageId, Instant nextAttemptAt, String errorMessage);

  void markTerminalFailure(UUID messageId, String errorMessage);
}
