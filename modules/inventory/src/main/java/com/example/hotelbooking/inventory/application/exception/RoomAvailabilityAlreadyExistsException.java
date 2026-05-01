package com.example.hotelbooking.inventory.application.exception;

import java.io.Serial;
import java.time.LocalDate;
import java.util.UUID;

public class RoomAvailabilityAlreadyExistsException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public RoomAvailabilityAlreadyExistsException(UUID hotelId, UUID roomTypeId, LocalDate date) {
    super(
        "Room availability already exists for hotelId=%s, roomTypeId=%s, date=%s"
            .formatted(hotelId, roomTypeId, date));
  }
}
