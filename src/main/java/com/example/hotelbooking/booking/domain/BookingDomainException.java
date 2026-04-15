package com.example.hotelbooking.booking.domain;

import java.io.Serial;

public class BookingDomainException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public BookingDomainException(String message) {
    super(message);
  }
}
