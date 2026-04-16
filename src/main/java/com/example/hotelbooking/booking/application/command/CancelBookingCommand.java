package com.example.hotelbooking.booking.application.command;

import com.example.hotelbooking.booking.domain.BookingId;
import java.util.Objects;

public record CancelBookingCommand(BookingId bookingId) {

  public CancelBookingCommand {
    Objects.requireNonNull(bookingId, "bookingId must not be null");
  }
}
