package com.example.hotelbooking.payment.domain;

import java.util.Locale;

public record PaymentCurrency(String value) {

  private static final int ISO_CODE_LENGTH = 3;

  public PaymentCurrency {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("payment currency must not be blank");
    }

    value = value.toUpperCase(Locale.ROOT);

    if (value.length() != ISO_CODE_LENGTH) {
      throw new IllegalArgumentException("payment currency must be a 3-letter ISO code");
    }
  }
}
