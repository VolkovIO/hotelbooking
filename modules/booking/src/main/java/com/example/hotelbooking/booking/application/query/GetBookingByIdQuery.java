package com.example.hotelbooking.booking.application.query;

import com.example.hotelbooking.booking.domain.BookingId;
import com.example.hotelbooking.booking.domain.UserId;
import java.util.Objects;

public record GetBookingByIdQuery(BookingId bookingId, UserId userId) {

  public GetBookingByIdQuery {
    Objects.requireNonNull(bookingId, "bookingId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
  }
}
