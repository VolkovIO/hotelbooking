package com.example.hotelbooking.notification.domain;

public record NotificationBody(String value) {

  public NotificationBody {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("notification body must not be blank");
    }
  }
}
