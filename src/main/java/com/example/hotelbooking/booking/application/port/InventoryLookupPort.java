package com.example.hotelbooking.booking.application.port;

import java.time.LocalDate;
import java.util.UUID;

public interface InventoryLookupPort {

  boolean hotelExists(UUID hotelId);

  boolean roomTypeExists(UUID hotelId, UUID roomTypeId);

  boolean isRoomTypeAvailable(UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut);
}
