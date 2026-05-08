package com.example.hotelbooking.payment.adapter.out.percistance.jpa;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile("payment-postgres")
@EntityScan(basePackageClasses = PaymentJpaEntity.class)
@EnableJpaRepositories(basePackageClasses = SpringDataPaymentJpaRepository.class)
class PaymentJpaPersistenceConfiguration {}
