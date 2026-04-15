package com.example.hotelbooking.booking.application.exception;

import java.io.Serial;
import java.util.UUID;

public class RoomTypeReferenceNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public RoomTypeReferenceNotFoundException(UUID hotelId, UUID roomTypeId) {
    super("Referenced room type does not exist in hotel " + hotelId + ": " + roomTypeId);
  }
}
