package com.example.hotelbooking.inventory.application.command;

import java.util.Objects;
import java.util.UUID;

public record AddRoomTypeCommand(UUID hotelId, String name, int guestCapacity) {

  public AddRoomTypeCommand {
    Objects.requireNonNull(hotelId, "hotelId must not be null");
    Objects.requireNonNull(name, "name must not be null");

    if (guestCapacity <= 0) {
      throw new IllegalArgumentException("guestCapacity must be positive");
    }
  }
}
