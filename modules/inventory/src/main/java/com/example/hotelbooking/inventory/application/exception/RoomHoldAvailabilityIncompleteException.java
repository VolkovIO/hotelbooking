package com.example.hotelbooking.inventory.application.exception;

import java.io.Serial;
import java.util.UUID;

public class RoomHoldAvailabilityIncompleteException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public RoomHoldAvailabilityIncompleteException(UUID holdId, long expectedDays, int actualDays) {
    super(
        "Room hold availability is incomplete for hold "
            + holdId
            + ": expected "
            + expectedDays
            + " days, but found "
            + actualDays);
  }
}
