package com.example.hotelbooking.inventory.application.service;

import com.example.hotelbooking.inventory.application.exception.RoomHoldAvailabilityIncompleteException;
import com.example.hotelbooking.inventory.application.exception.RoomHoldNotFoundException;
import com.example.hotelbooking.inventory.application.port.in.ConfirmRoomHoldUseCase;
import com.example.hotelbooking.inventory.application.port.out.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.application.port.out.RoomHoldRepository;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import com.example.hotelbooking.inventory.domain.RoomHold;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfirmRoomHoldService implements ConfirmRoomHoldUseCase {

  private final RoomHoldRepository roomHoldRepository;
  private final RoomAvailabilityRepository roomAvailabilityRepository;

  @Override
  public void execute(UUID holdId) {
    RoomHold roomHold =
        roomHoldRepository
            .findById(holdId)
            .orElseThrow(() -> new RoomHoldNotFoundException(holdId));

    List<RoomAvailability> availabilityList =
        roomAvailabilityRepository.findByRoomTypeAndDateRange(
            roomHold.getHotelId(),
            roomHold.getRoomTypeId(),
            roomHold.getCheckIn(),
            roomHold.getCheckOut().minusDays(1));

    validateCompleteAvailabilityRange(roomHold, availabilityList);

    List<RoomAvailability> updatedAvailability =
        availabilityList.stream()
            .map(availability -> availability.confirmHold(roomHold.getRooms()))
            .toList();

    roomAvailabilityRepository.saveAll(updatedAvailability);
    roomHoldRepository.deleteById(holdId);
  }

  private void validateCompleteAvailabilityRange(
      RoomHold roomHold, List<RoomAvailability> availabilityList) {
    long expectedDays = ChronoUnit.DAYS.between(roomHold.getCheckIn(), roomHold.getCheckOut());
    if (availabilityList.size() != expectedDays) {
      throw new RoomHoldAvailabilityIncompleteException(
          roomHold.getId(), expectedDays, availabilityList.size());
    }
  }
}
