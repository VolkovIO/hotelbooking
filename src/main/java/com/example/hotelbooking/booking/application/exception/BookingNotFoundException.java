package com.example.hotelbooking.booking.application.exception;

import com.example.hotelbooking.booking.domain.BookingId;
import java.io.Serial;

public class BookingNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public BookingNotFoundException(BookingId bookingId) {
    super("Booking not found: " + bookingId);
  }
}
