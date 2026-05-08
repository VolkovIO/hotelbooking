package com.example.hotelbooking.payment.domain;

public record PaymentProviderPaymentId(String value) {

  private static final int MAX_LENGTH = 128;

  public PaymentProviderPaymentId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("provider payment id must not be blank");
    }

    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("provider payment id is too long");
    }
  }
}
