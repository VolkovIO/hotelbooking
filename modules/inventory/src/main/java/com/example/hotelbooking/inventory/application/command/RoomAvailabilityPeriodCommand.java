package com.example.hotelbooking.inventory.application.command;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record RoomAvailabilityPeriodCommand(
    UUID hotelId, UUID roomTypeId, LocalDate startDate, LocalDate endDate, int totalRooms) {

  public RoomAvailabilityPeriodCommand {
    Objects.requireNonNull(hotelId, "hotelId must not be null");
    Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
    Objects.requireNonNull(startDate, "startDate must not be null");
    Objects.requireNonNull(endDate, "endDate must not be null");

    if (endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("endDate must not be before startDate");
    }

    if (totalRooms <= 0) {
      throw new IllegalArgumentException("totalRooms must be positive");
    }
  }
}
