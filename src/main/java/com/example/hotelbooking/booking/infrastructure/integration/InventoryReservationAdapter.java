package com.example.hotelbooking.booking.infrastructure.integration;

import com.example.hotelbooking.booking.application.port.InventoryReservationPort;
import com.example.hotelbooking.inventory.application.command.ReleaseRoomHoldUseCase;
import com.example.hotelbooking.inventory.application.port.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.application.port.RoomHoldRepository;
import com.example.hotelbooking.inventory.domain.InventoryDomainException;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import com.example.hotelbooking.inventory.domain.RoomHold;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryReservationAdapter implements InventoryReservationPort {

  private final RoomAvailabilityRepository roomAvailabilityRepository;
  private final RoomHoldRepository roomHoldRepository;
  private final ReleaseRoomHoldUseCase releaseRoomHoldUseCase;

  @Override
  public UUID placeHold(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    LocalDate availabilityTo = checkOut.minusDays(1);

    if (availabilityTo.isBefore(checkIn)) {
      throw new InventoryDomainException("Invalid hold period");
    }

    long requiredDays = ChronoUnit.DAYS.between(checkIn, checkOut);

    List<RoomAvailability> availabilityList =
        roomAvailabilityRepository.findByRoomTypeAndDateRange(
            hotelId, roomTypeId, checkIn, availabilityTo);

    if (availabilityList.size() != requiredDays) {
      throw new InventoryDomainException("Availability is not configured for the full stay period");
    }

    List<RoomAvailability> updatedAvailability =
        availabilityList.stream().map(item -> item.placeHold(rooms)).toList();

    roomAvailabilityRepository.saveAll(updatedAvailability);

    RoomHold roomHold = RoomHold.create(hotelId, roomTypeId, checkIn, checkOut, rooms);
    roomHoldRepository.save(roomHold);

    return roomHold.getId();
  }

  @Override
  public void releaseHold(UUID holdId) {
    releaseRoomHoldUseCase.execute(holdId);
  }
}
