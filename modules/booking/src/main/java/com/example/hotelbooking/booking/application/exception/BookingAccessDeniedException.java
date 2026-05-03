package com.example.hotelbooking.booking.application.exception;

import com.example.hotelbooking.booking.domain.BookingId;
import java.io.Serial;

public class BookingAccessDeniedException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public BookingAccessDeniedException(BookingId bookingId) {
    super("Access denied for booking: " + bookingId);
  }
}
