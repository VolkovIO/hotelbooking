package com.example.hotelbooking.inventory.application.exception;

import java.io.Serial;
import java.util.UUID;

public class HotelNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public HotelNotFoundException(UUID hotelId) {
    super("Hotel not found: " + hotelId);
  }
}
