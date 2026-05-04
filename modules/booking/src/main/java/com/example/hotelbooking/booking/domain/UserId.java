package com.example.hotelbooking.booking.domain;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

  public UserId {
    Objects.requireNonNull(value, "userId must not be null");
  }

  public static UserId from(String value) {
    return new UserId(UUID.fromString(value));
  }

  public static UserId newId() {
    return new UserId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
