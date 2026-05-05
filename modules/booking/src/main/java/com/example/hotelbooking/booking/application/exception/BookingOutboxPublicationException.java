package com.example.hotelbooking.booking.application.exception;

import java.io.Serial;

public class BookingOutboxPublicationException extends Exception {

  @Serial private static final long serialVersionUID = 1L;

  public BookingOutboxPublicationException(String message) {
    super(message);
  }

  public BookingOutboxPublicationException(String message, Throwable cause) {
    super(message, cause);
  }
}
