package com.example.hotelbooking.notification.domain;

public record NotificationErrorMessage(String value) {

  private static final int MAX_LENGTH = 2_000;

  public NotificationErrorMessage {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("notification error message must not be blank");
    }

    if (value.length() > MAX_LENGTH) {
      value = value.substring(0, MAX_LENGTH);
    }
  }
}
