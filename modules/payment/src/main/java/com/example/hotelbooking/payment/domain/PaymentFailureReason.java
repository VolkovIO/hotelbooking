package com.example.hotelbooking.payment.domain;

public record PaymentFailureReason(String value) {

  private static final int MAX_LENGTH = 1000;

  public PaymentFailureReason {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("payment failure reason must not be blank");
    }

    if (value.length() > MAX_LENGTH) {
      value = value.substring(0, MAX_LENGTH);
    }
  }
}
