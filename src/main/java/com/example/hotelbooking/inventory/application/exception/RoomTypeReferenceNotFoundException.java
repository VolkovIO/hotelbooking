package com.example.hotelbooking.inventory.application.exception;

import java.io.Serial;
import java.util.UUID;

public class RoomTypeReferenceNotFoundException extends InventoryApplicationException {

  @Serial private static final long serialVersionUID = 1L;

  public RoomTypeReferenceNotFoundException(UUID hotelId, UUID roomTypeId) {
    super(
        "Room type reference not found for hotel %s and room type %s"
            .formatted(hotelId, roomTypeId));
  }
}
