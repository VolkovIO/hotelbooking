package com.example.hotelbooking.booking.domain;

import java.util.Objects;
import java.util.UUID;

public record BookingId(UUID value) {

  public BookingId {
    Objects.requireNonNull(value, "value must not be null");
  }

  public static BookingId newId() {
    return new BookingId(UUID.randomUUID());
  }

  public static BookingId from(String value) {
    return new BookingId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
