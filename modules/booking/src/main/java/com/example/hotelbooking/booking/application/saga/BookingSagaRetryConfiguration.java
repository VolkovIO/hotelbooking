package com.example.hotelbooking.booking.application.saga;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BookingSagaRetryProperties.class)
class BookingSagaRetryConfiguration {}
