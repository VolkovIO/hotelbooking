package com.example.hotelbooking.booking.application.command;

import com.example.hotelbooking.booking.domain.UserId;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record CreateBookingCommand(
    UserId userId,
    UUID hotelId,
    UUID roomTypeId,
    LocalDate checkIn,
    LocalDate checkOut,
    int guestCount) {

  public CreateBookingCommand {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(hotelId, "hotelId must not be null");
    Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
    Objects.requireNonNull(checkIn, "checkIn must not be null");
    Objects.requireNonNull(checkOut, "checkOut must not be null");

    if (guestCount <= 0) {
      throw new IllegalArgumentException("guestCount must be positive");
    }
  }
}
