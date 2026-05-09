package com.example.hotelbooking.booking.adapter.out.integration.payment.http;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaymentHttpClientProperties.class)
class PaymentHttpClientConfiguration {}
