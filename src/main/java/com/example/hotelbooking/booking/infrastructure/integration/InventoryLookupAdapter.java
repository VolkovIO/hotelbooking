package com.example.hotelbooking.booking.infrastructure.integration;

import com.example.hotelbooking.booking.application.port.InventoryLookupPort;
import com.example.hotelbooking.inventory.application.port.HotelRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryLookupAdapter implements InventoryLookupPort {

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
}
