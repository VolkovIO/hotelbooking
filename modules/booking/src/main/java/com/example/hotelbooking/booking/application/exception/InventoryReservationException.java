package com.example.hotelbooking.booking.application.exception;

import java.io.Serial;

public class InventoryReservationException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public InventoryReservationException(String message, Throwable cause) {
    super(message, cause);
  }

  public InventoryReservationException(String message) {
    super(message);
  }
}
