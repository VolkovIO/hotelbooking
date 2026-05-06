package com.example.hotelbooking.notification.domain;

public record NotificationAttemptCount(int value) {

  public NotificationAttemptCount {
    if (value < 0) {
      throw new IllegalArgumentException("notification attempts must not be negative");
    }
  }

  public static NotificationAttemptCount zero() {
    return new NotificationAttemptCount(0);
  }

  public NotificationAttemptCount increment() {
    return new NotificationAttemptCount(value + 1);
  }
}
