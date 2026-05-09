package com.example.hotelbooking.booking.application.saga;

import java.util.Objects;
import java.util.UUID;

public record BookingSagaId(UUID value) {

  public BookingSagaId {
    Objects.requireNonNull(value, "booking saga id must not be null");
  }

  public static BookingSagaId newId() {
    return new BookingSagaId(UUID.randomUUID());
  }
}
