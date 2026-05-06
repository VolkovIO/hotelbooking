package com.example.hotelbooking.notification.domain;

public record NotificationDestination(String value) {

  private static final int MAX_LENGTH = 512;

  public NotificationDestination {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("notification destination must not be blank");
    }

    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("notification destination is too long");
    }
  }
}
