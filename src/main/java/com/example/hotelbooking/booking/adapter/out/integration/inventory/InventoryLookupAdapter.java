package com.example.hotelbooking.booking.adapter.out.integration.inventory;

import com.example.hotelbooking.booking.application.port.out.InventoryLookupPort;
import com.example.hotelbooking.inventory.application.port.out.HotelRepository;
import java.util.OptionalInt;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class InventoryLookupAdapter implements InventoryLookupPort {

  private final HotelRepository hotelRepository;

  @Override
  public boolean hotelExists(UUID hotelId) {
    return hotelRepository.findById(hotelId).isPresent();
  }

  @Override
  public boolean roomTypeExists(UUID hotelId, UUID roomTypeId) {
    return hotelRepository
        .findById(hotelId)
        .map(
            hotel ->
                hotel.getRoomTypes().stream()
                    .anyMatch(roomType -> roomType.getId().equals(roomTypeId)))
        .orElse(false);
  }

  @Override
  public OptionalInt findRoomTypeGuestCapacity(UUID hotelId, UUID roomTypeId) {
    return hotelRepository
        .findById(hotelId)
        .flatMap(
            hotel ->
                hotel.getRoomTypes().stream()
                    .filter(roomType -> roomType.getId().equals(roomTypeId))
                    .findFirst())
        .map(roomType -> OptionalInt.of(roomType.getGuestCapacity()))
        .orElseGet(OptionalInt::empty);
  }
}
