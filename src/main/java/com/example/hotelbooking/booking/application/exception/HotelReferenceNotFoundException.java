package com.example.hotelbooking.booking.application.exception;

import java.io.Serial;
import java.util.UUID;

public class HotelReferenceNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public HotelReferenceNotFoundException(UUID hotelId) {
    super("Referenced hotel does not exist: " + hotelId);
  }
}
