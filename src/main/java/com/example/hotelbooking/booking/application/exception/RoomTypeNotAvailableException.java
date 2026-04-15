package com.example.hotelbooking.booking.application.exception;

import java.io.Serial;
import java.util.UUID;

public class RoomTypeNotAvailableException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public RoomTypeNotAvailableException(UUID hotelId, UUID roomTypeId) {
    super("Room type is not available in hotel " + hotelId + ": " + roomTypeId);
  }
}
