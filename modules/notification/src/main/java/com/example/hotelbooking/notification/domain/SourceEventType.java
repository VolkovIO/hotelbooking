package com.example.hotelbooking.notification.domain;

public record SourceEventType(String value) {

  private static final int MAX_LENGTH = 128;

  public SourceEventType {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("source event type must not be blank");
    }

    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("source event type is too long");
    }
  }
}
