package com.example.hotelbooking.booking.application.exception;

import java.io.Serial;

public class RoomHoldFailedException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public RoomHoldFailedException(String message) {
    super(message);
  }

  public RoomHoldFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
