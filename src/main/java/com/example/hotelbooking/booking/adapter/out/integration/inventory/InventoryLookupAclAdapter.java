package com.example.hotelbooking.booking.adapter.out.integration.inventory;

import com.example.hotelbooking.booking.application.port.out.InventoryLookupPort;
import com.example.hotelbooking.inventory.application.port.in.InventoryQueryUseCase;
import java.util.OptionalInt;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class InventoryLookupAclAdapter implements InventoryLookupPort {

  private final InventoryQueryUseCase inventoryQueryUseCase;

  @Override
  public boolean hotelExists(UUID hotelId) {
    return inventoryQueryUseCase.hotelExists(hotelId);
  }

  @Override
  public boolean roomTypeExists(UUID hotelId, UUID roomTypeId) {
    return inventoryQueryUseCase.roomTypeExists(hotelId, roomTypeId);
  }

  @Override
  public OptionalInt findRoomTypeGuestCapacity(UUID hotelId, UUID roomTypeId) {
    return inventoryQueryUseCase.findRoomTypeGuestCapacity(hotelId, roomTypeId);
  }
}
