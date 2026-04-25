package com.example.hotelbooking.inventory.application.port.in;

import java.util.OptionalInt;
import java.util.UUID;

public interface InventoryQueryUseCase {

  boolean hotelExists(UUID hotelId);

  boolean roomTypeExists(UUID hotelId, UUID roomTypeId);

  OptionalInt findRoomTypeGuestCapacity(UUID hotelId, UUID roomTypeId);
}
