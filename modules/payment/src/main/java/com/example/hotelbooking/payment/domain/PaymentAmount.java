package com.example.hotelbooking.payment.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record PaymentAmount(BigDecimal value) {

  private static final int MAX_SCALE = 2;

  public PaymentAmount {
    Objects.requireNonNull(value, "payment amount must not be null");

    if (value.signum() <= 0) {
      throw new IllegalArgumentException("payment amount must be positive");
    }

    if (value.scale() > MAX_SCALE) {
      throw new IllegalArgumentException("payment amount scale must not be greater than 2");
    }
  }
}
