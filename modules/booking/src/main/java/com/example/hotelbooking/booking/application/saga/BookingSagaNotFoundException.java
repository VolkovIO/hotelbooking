package com.example.hotelbooking.booking.application.saga;

import java.io.Serial;

public class BookingSagaNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public BookingSagaNotFoundException(BookingSagaId sagaId) {
    super("Booking saga was not found: sagaId=" + sagaId.value());
  }
}
