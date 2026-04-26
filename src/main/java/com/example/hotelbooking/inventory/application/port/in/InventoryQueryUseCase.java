package com.example.hotelbooking.inventory.application.port.in;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface InventoryQueryUseCase {

  Optional<RoomTypeReferenceResult> findRoomTypeReference(UUID hotelId, UUID roomTypeId);
}
