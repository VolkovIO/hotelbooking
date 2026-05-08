package com.example.hotelbooking.payment.application.outbox;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Profile("payment-outbox-publisher")
class PaymentOutboxSchedulingConfiguration {}
