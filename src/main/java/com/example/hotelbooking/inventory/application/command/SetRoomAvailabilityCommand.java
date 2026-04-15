package com.example.hotelbooking.inventory.application.command;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record SetRoomAvailabilityCommand(
    UUID hotelId, UUID roomTypeId, LocalDate from, LocalDate to, int totalRooms) {

  public SetRoomAvailabilityCommand {
    Objects.requireNonNull(hotelId, "hotelId must not be null");
    Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
    Objects.requireNonNull(from, "from must not be null");
    Objects.requireNonNull(to, "to must not be null");

    if (totalRooms < 0) {
      throw new IllegalArgumentException("totalRooms must not be negative");
    }
  }
}
