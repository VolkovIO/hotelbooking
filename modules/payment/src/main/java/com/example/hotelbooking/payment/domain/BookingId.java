package com.example.hotelbooking.payment.domain;

import java.util.Objects;
import java.util.UUID;

public record BookingId(UUID value) {

  public BookingId {
    Objects.requireNonNull(value, "booking id must not be null");
  }
}
