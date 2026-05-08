package com.example.hotelbooking.payment.domain;

import java.util.Objects;
import java.util.UUID;

public record PaymentUserId(UUID value) {

  public PaymentUserId {
    Objects.requireNonNull(value, "payment user id must not be null");
  }
}
