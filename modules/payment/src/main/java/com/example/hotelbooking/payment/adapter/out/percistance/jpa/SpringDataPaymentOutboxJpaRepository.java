package com.example.hotelbooking.payment.adapter.out.percistance.jpa;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataPaymentOutboxJpaRepository extends JpaRepository<PaymentOutboxJpaEntity, UUID> {

  @Query(
      value =
          """
          select *
          from payment_outbox
          where processing_status = 'NEW'
            and next_attempt_at <= :now
          order by occurred_at
          limit :batchSize
          for update skip locked
          """,
      nativeQuery = true)
  List<PaymentOutboxJpaEntity> findReadyForProcessing(
      @Param("now") Instant now, @Param("batchSize") int batchSize);
}
