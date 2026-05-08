package com.example.hotelbooking.payment.application.port.out;

import com.example.hotelbooking.payment.application.outbox.PaymentOutboxMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentOutboxRepository {

  PaymentOutboxMessage save(PaymentOutboxMessage message);

  List<PaymentOutboxMessage> claimBatchForProcessing(Instant now, int batchSize);

  void markPublished(UUID eventId, Instant publishedAt);

  void markRetryableFailure(UUID eventId, String errorMessage, Instant nextAttemptAt);

  void markTerminalFailure(UUID eventId, String errorMessage);
}
