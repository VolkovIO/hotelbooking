package com.example.hotelbooking.payment.adapter.out.percistance.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPaymentOutboxJpaRepository
    extends JpaRepository<PaymentOutboxJpaEntity, UUID> {}
