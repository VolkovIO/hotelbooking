package com.example.hotelbooking.payment.adapter.out.percistance.jpa;

import com.example.hotelbooking.payment.application.outbox.PaymentOutboxMessage;
import com.example.hotelbooking.payment.application.port.out.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("payment-postgres")
@RequiredArgsConstructor
class JpaPaymentOutboxRepository implements PaymentOutboxRepository {

  private final SpringDataPaymentOutboxJpaRepository springDataRepository;

  @Override
  public PaymentOutboxMessage save(PaymentOutboxMessage message) {
    return springDataRepository.save(PaymentOutboxJpaEntity.from(message)).toDomain();
  }
}
