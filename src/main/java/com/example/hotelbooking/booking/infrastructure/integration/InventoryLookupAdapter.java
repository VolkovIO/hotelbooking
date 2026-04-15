package com.example.hotelbooking.booking.infrastructure.integration;

import com.example.hotelbooking.booking.application.port.InventoryLookupPort;
import com.example.hotelbooking.inventory.application.port.HotelRepository;
import com.example.hotelbooking.inventory.application.port.RoomAvailabilityRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryLookupAdapter implements InventoryLookupPort {

  private final HotelRepository hotelRepository;
  private final RoomAvailabilityRepository roomAvailabilityRepository;

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
  public boolean isRoomTypeAvailable(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut) {
    LocalDate availabilityTo = checkOut.minusDays(1);

    if (availabilityTo.isBefore(checkIn)) {
      return false;
    }

    long requiredDays = ChronoUnit.DAYS.between(checkIn, checkOut);

    var availabilityList =
        roomAvailabilityRepository.findByRoomTypeAndDateRange(
            hotelId, roomTypeId, checkIn, availabilityTo);

    if (availabilityList.size() != requiredDays) {
      return false;
    }

    return availabilityList.stream().allMatch(item -> item.getTotalRooms() > 0);
  }
}
