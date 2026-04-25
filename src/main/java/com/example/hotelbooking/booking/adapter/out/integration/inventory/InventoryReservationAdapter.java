package com.example.hotelbooking.booking.adapter.out.integration.inventory;

import com.example.hotelbooking.booking.application.exception.RoomHoldFailedException;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.inventory.application.command.ConfirmRoomHoldUseCase;
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
final class InventoryReservationAdapter implements InventoryReservationPort {

  private final RoomAvailabilityRepository roomAvailabilityRepository;
  private final RoomHoldRepository roomHoldRepository;
  private final ReleaseRoomHoldUseCase releaseRoomHoldUseCase;
  private final ConfirmRoomHoldUseCase confirmRoomHoldUseCase;

  @Override
  public UUID placeHold(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    try {
      LocalDate availabilityTo = checkOut.minusDays(1);

      if (availabilityTo.isBefore(checkIn)) {
        throw new InventoryDomainException("Invalid hold period");
      }

      long requiredDays = ChronoUnit.DAYS.between(checkIn, checkOut);

      List<RoomAvailability> availabilityList =
          roomAvailabilityRepository.findByRoomTypeAndDateRange(
              hotelId, roomTypeId, checkIn, availabilityTo);

      if (availabilityList.size() != requiredDays) {
        throw new InventoryDomainException(
            "Availability is not configured for the full stay period");
      }

      List<RoomAvailability> updatedAvailability =
          availabilityList.stream().map(item -> item.placeHold(rooms)).toList();

      roomAvailabilityRepository.saveAll(updatedAvailability);

      RoomHold roomHold = RoomHold.create(hotelId, roomTypeId, checkIn, checkOut, rooms);
      roomHoldRepository.save(roomHold);

      return roomHold.getId();
    } catch (InventoryDomainException exception) {
      throw new RoomHoldFailedException(
          "Failed to place room hold for hotel %s and room type %s".formatted(hotelId, roomTypeId),
          exception);
    }
  }

  @Override
  public void releaseHold(UUID holdId) {
    releaseRoomHoldUseCase.execute(holdId);
  }

  @Override
  public void confirmHold(UUID holdId) {
    confirmRoomHoldUseCase.execute(holdId);
  }
}
