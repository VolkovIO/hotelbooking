package com.example.hotelbooking.booking.adapter.out.integration.payment.http;

import java.math.BigDecimal;
import java.util.UUID;

record PaymentHttpResponse(
    UUID paymentId,
    UUID bookingId,
    UUID userId,
    BigDecimal amount,
    String currency,
    String status,
    String provider,
    String providerPaymentId,
    String failureReason) {}
