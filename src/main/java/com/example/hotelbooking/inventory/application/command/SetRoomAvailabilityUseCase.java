package com.example.hotelbooking.inventory.application.command;

import com.example.hotelbooking.inventory.application.exception.HotelNotFoundException;
import com.example.hotelbooking.inventory.application.port.HotelRepository;
import com.example.hotelbooking.inventory.application.port.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import com.example.hotelbooking.inventory.domain.InventoryDomainException;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SetRoomAvailabilityUseCase {

  private final HotelRepository hotelRepository;
  private final RoomAvailabilityRepository roomAvailabilityRepository;

  public void execute(SetRoomAvailabilityCommand command) {
    Hotel hotel =
        hotelRepository
            .findById(command.hotelId())
            .orElseThrow(() -> new HotelNotFoundException(command.hotelId()));

    boolean roomTypeExists =
        hotel.getRoomTypes().stream()
            .anyMatch(roomType -> roomType.getId().equals(command.roomTypeId()));

    if (!roomTypeExists) {
      throw new InventoryDomainException("Room type does not belong to the specified hotel");
    }

    if (command.to().isBefore(command.from())) {
      throw new InventoryDomainException("to must not be before from");
    }

    List<RoomAvailability> availabilityList = new ArrayList<>();

    LocalDate current = command.from();
    while (!current.isAfter(command.to())) {
      availabilityList.add(
          RoomAvailability.create(
              command.hotelId(), command.roomTypeId(), current, command.totalRooms()));
      current = current.plusDays(1);
    }

    roomAvailabilityRepository.saveAll(availabilityList);
  }
}
