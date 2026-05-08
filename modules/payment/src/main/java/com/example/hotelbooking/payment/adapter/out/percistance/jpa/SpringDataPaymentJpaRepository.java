package com.example.hotelbooking.payment.adapter.out.percistance.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

  Optional<PaymentJpaEntity> findByBookingId(UUID bookingId);
}
