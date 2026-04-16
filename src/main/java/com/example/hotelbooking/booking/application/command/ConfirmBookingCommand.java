package com.example.hotelbooking.booking.application.command;

import com.example.hotelbooking.booking.domain.BookingId;
import java.util.Objects;

public record ConfirmBookingCommand(BookingId bookingId) {

  public ConfirmBookingCommand {
    Objects.requireNonNull(bookingId, "bookingId must not be null");
  }
}
