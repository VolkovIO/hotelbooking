package com.example.hotelbooking.booking.application.port;

import java.util.OptionalInt;
import java.util.UUID;

public interface InventoryLookupPort {

  boolean hotelExists(UUID hotelId);

  boolean roomTypeExists(UUID hotelId, UUID roomTypeId);

  OptionalInt findRoomTypeGuestCapacity(UUID hotelId, UUID roomTypeId);
}
