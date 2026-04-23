package com.example.hotelbooking.inventory.application.command;

import com.example.hotelbooking.inventory.application.exception.HotelNotFoundException;
import com.example.hotelbooking.inventory.application.exception.RoomAvailabilityAlreadyExistsException;
import com.example.hotelbooking.inventory.application.port.HotelRepository;
import com.example.hotelbooking.inventory.application.port.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InitializeRoomAvailabilityUseCase {

  private final HotelRepository hotelRepository;
  private final RoomAvailabilityRepository roomAvailabilityRepository;

  public void execute(RoomAvailabilityPeriodCommand command) {
    Hotel hotel =
        hotelRepository
            .findById(command.hotelId())
            .orElseThrow(() -> new HotelNotFoundException(command.hotelId()));

    hotel.requireRoomType(command.roomTypeId());

    validateRangeDoesNotExist(
        command.hotelId(), command.roomTypeId(), command.startDate(), command.endDate());
    createRange(
        command.hotelId(),
        command.roomTypeId(),
        command.startDate(),
        command.endDate(),
        command.totalRooms());
  }

  private void validateRangeDoesNotExist(
      UUID hotelId, UUID roomTypeId, LocalDate startDate, LocalDate endDate) {

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      if (roomAvailabilityRepository
          .findByHotelIdAndRoomTypeIdAndDate(hotelId, roomTypeId, date)
          .isPresent()) {
        throw new RoomAvailabilityAlreadyExistsException(hotelId, roomTypeId, date);
      }
    }
  }

  private void createRange(
      UUID hotelId, UUID roomTypeId, LocalDate startDate, LocalDate endDate, int totalRooms) {

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      roomAvailabilityRepository.save(
          RoomAvailability.create(hotelId, roomTypeId, date, totalRooms));
    }
  }
}
