package com.example.hotelbooking.payment.adapter.out.percistance.jpa;

import com.example.hotelbooking.payment.application.outbox.PaymentOutboxMessage;
import com.example.hotelbooking.payment.application.port.out.PaymentOutboxRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("payment-postgres")
@RequiredArgsConstructor
class JpaPaymentOutboxRepository implements PaymentOutboxRepository {

  private final SpringDataPaymentOutboxJpaRepository springDataRepository;

  @Override
  public PaymentOutboxMessage save(PaymentOutboxMessage message) {
    return springDataRepository.save(PaymentOutboxJpaEntity.from(message)).toDomain();
  }

  @Override
  @Transactional
  public List<PaymentOutboxMessage> claimBatchForProcessing(Instant now, int batchSize) {
    List<PaymentOutboxJpaEntity> entities =
        springDataRepository.findReadyForProcessing(now, batchSize);

    entities.forEach(entity -> entity.markProcessing(now));

    return entities.stream().map(PaymentOutboxJpaEntity::toDomain).toList();
  }

  @Override
  @Transactional
  public void markPublished(UUID eventId, Instant publishedAt) {
    springDataRepository.findById(eventId).ifPresent(entity -> entity.markPublished(publishedAt));
  }

  @Override
  @Transactional
  public void markRetryableFailure(UUID eventId, String errorMessage, Instant nextAttemptAt) {
    Instant now = Instant.now();

    springDataRepository
        .findById(eventId)
        .ifPresent(entity -> entity.markRetryableFailure(errorMessage, nextAttemptAt, now));
  }

  @Override
  @Transactional
  public void markTerminalFailure(UUID eventId, String errorMessage) {
    Instant now = Instant.now();

    springDataRepository
        .findById(eventId)
        .ifPresent(entity -> entity.markTerminalFailure(errorMessage, now));
  }
}
