package com.example.hotelbooking.inventory.application.command;

import com.example.hotelbooking.inventory.application.exception.RoomHoldNotFoundException;
import com.example.hotelbooking.inventory.application.port.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.application.port.RoomHoldRepository;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import com.example.hotelbooking.inventory.domain.RoomHold;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfirmRoomHoldUseCase {

  private final RoomHoldRepository roomHoldRepository;
  private final RoomAvailabilityRepository roomAvailabilityRepository;

  public void execute(UUID holdId) {
    RoomHold roomHold =
        roomHoldRepository
            .findById(holdId)
            .orElseThrow(() -> new RoomHoldNotFoundException(holdId));

    LocalDate availabilityTo = roomHold.getCheckOut().minusDays(1);

    List<RoomAvailability> availabilityList =
        roomAvailabilityRepository.findByRoomTypeAndDateRange(
            roomHold.getHotelId(), roomHold.getRoomTypeId(), roomHold.getCheckIn(), availabilityTo);

    List<RoomAvailability> updatedAvailability =
        availabilityList.stream().map(item -> item.confirmHold(roomHold.getRooms())).toList();

    roomAvailabilityRepository.saveAll(updatedAvailability);
    roomHoldRepository.deleteById(holdId);
  }
}
