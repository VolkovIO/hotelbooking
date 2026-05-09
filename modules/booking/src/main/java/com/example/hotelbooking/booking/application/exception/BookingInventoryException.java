package com.example.hotelbooking.booking.application.exception;

import java.io.Serial;

public abstract class BookingInventoryException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  protected BookingInventoryException(String message, Throwable cause) {
    super(message, cause);
  }

  protected BookingInventoryException(String message) {
    super(message);
  }
}
