package com.example.hotelbooking.booking.application.saga;

public record BookingSagaFailureReason(String value) {

  private static final int MAX_LENGTH = 1000;

  public BookingSagaFailureReason {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("booking saga failure reason must not be blank");
    }

    if (value.length() > MAX_LENGTH) {
      value = value.substring(0, MAX_LENGTH);
    }
  }
}
