package com.example.hotelbooking.booking.adapter.out.integration.inventory;

import com.example.hotelbooking.booking.application.port.out.InventoryLookupPort;
import com.example.hotelbooking.booking.application.port.out.RoomTypeReference;
import com.example.hotelbooking.inventory.application.port.in.InventoryQueryUseCase;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("inventory-direct-client")
@RequiredArgsConstructor
final class InventoryLookupAclAdapter implements InventoryLookupPort {

  private final InventoryQueryUseCase inventoryQueryUseCase;

  @Override
  public Optional<RoomTypeReference> findRoomTypeReference(UUID hotelId, UUID roomTypeId) {
    return inventoryQueryUseCase
        .findRoomTypeReference(hotelId, roomTypeId)
        .map(
            result ->
                new RoomTypeReference(
                    result.hotelId(), result.roomTypeId(), result.guestCapacity()));
  }
}
