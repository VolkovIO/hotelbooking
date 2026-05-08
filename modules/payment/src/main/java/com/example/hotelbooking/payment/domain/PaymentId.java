package com.example.hotelbooking.payment.domain;

import java.util.Objects;
import java.util.UUID;

public record PaymentId(UUID value) {

  public PaymentId {
    Objects.requireNonNull(value, "payment id must not be null");
  }

  public static PaymentId newId() {
    return new PaymentId(UUID.randomUUID());
  }
}
