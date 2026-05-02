package com.example.hotelbooking.booking.application.port.out;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface InventoryLookupPort {

  Optional<RoomTypeReference> findRoomTypeReference(UUID hotelId, UUID roomTypeId);
}
