package com.example.hotelbooking.notification.domain;

public record NotificationSubject(String value) {

  private static final int MAX_LENGTH = 255;

  public NotificationSubject {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("notification subject must not be blank");
    }

    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("notification subject is too long");
    }
  }
}
