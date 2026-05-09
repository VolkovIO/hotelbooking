package com.example.hotelbooking.booking.application.payment;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record PaymentResult(
    UUID paymentId,
    UUID bookingId,
    UUID userId,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    String provider,
    String providerPaymentId,
    String failureReason) {

  public PaymentResult {
    Objects.requireNonNull(paymentId, "paymentId must not be null");
    Objects.requireNonNull(bookingId, "bookingId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(amount, "amount must not be null");
    Objects.requireNonNull(currency, "currency must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(provider, "provider must not be null");
  }

  public boolean isAuthorized() {
    return status == PaymentStatus.AUTHORIZED;
  }

  public boolean isDeclined() {
    return status == PaymentStatus.DECLINED;
  }
}
