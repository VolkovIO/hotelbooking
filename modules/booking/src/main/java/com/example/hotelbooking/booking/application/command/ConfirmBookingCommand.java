package com.example.hotelbooking.booking.application.command;

import com.example.hotelbooking.booking.domain.BookingId;
import com.example.hotelbooking.booking.domain.UserId;
import java.util.Objects;

public record ConfirmBookingCommand(BookingId bookingId, UserId userId) {

  public ConfirmBookingCommand {
    Objects.requireNonNull(bookingId, "bookingId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
  }
}
