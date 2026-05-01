package com.example.hotelbooking.inventory.application.service;

import com.example.hotelbooking.inventory.application.port.in.InventoryQueryUseCase;
import com.example.hotelbooking.inventory.application.port.in.RoomTypeReferenceResult;
import com.example.hotelbooking.inventory.application.port.out.HotelRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryQueryService implements InventoryQueryUseCase {

  private final HotelRepository hotelRepository;

  @Override
  public Optional<RoomTypeReferenceResult> findRoomTypeReference(UUID hotelId, UUID roomTypeId) {
    return hotelRepository
        .findById(hotelId)
        .flatMap(
            hotel ->
                hotel.getRoomTypes().stream()
                    .filter(roomType -> roomType.getId().equals(roomTypeId))
                    .findFirst()
                    .map(
                        roomType ->
                            new RoomTypeReferenceResult(
                                hotelId, roomTypeId, roomType.getGuestCapacity())));
  }
}
