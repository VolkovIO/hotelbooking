package com.example.hotelbooking.booking.adapter.out.integration.payment.http;

import java.math.BigDecimal;
import java.util.UUID;

record PaymentAuthorizeHttpRequest(
    UUID bookingId, UUID userId, BigDecimal amount, String currency) {}
