package com.example.hotelbooking.inventory.application.exception;

import java.io.Serial;
import java.util.UUID;

public class RoomHoldNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public RoomHoldNotFoundException(UUID holdId) {
    super("Room hold not found: " + holdId);
  }
}
