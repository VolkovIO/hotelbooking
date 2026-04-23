package com.example.hotelbooking.booking.application.exception;

import java.io.Serial;

public class GuestCountExceedsRoomCapacityException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public GuestCountExceedsRoomCapacityException(int guestCount, int guestCapacity) {
    super("Guest count " + guestCount + " exceeds room type capacity " + guestCapacity);
  }
}
