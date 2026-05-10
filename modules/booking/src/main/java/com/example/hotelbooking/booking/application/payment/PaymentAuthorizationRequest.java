package com.example.hotelbooking.booking.application.payment;

import com.example.hotelbooking.booking.domain.UserId;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record PaymentAuthorizationRequest(
    UUID bookingId, UserId userId, BigDecimal amount, String currency) {

  public PaymentAuthorizationRequest {
    Objects.requireNonNull(bookingId, "bookingId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(amount, "amount must not be null");
    Objects.requireNonNull(currency, "currency must not be null");

    if (amount.signum() <= 0) {
      throw new IllegalArgumentException("amount must be positive");
    }

    if (currency.isBlank()) {
      throw new IllegalArgumentException("currency must not be blank");
    }
  }
}
