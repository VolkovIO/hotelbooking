package com.example.hotelbooking.payment.application.command;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record AuthorizePaymentCommand(
    UUID bookingId, UUID userId, BigDecimal amount, String currency) {

  public AuthorizePaymentCommand {
    Objects.requireNonNull(bookingId, "bookingId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(amount, "amount must not be null");

    if (currency == null || currency.isBlank()) {
      throw new IllegalArgumentException("currency must not be blank");
    }
  }
}
