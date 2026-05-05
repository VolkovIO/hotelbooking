package com.example.hotelbooking.bookingservice.outbox;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Profile("outbox-publisher")
class OutboxSchedulingConfig {}
