package com.example.hotelbooking.inventory.application.command;

import com.example.hotelbooking.inventory.application.exception.HotelNotFoundException;
import com.example.hotelbooking.inventory.application.exception.RoomAvailabilityNotFoundException;
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
public class AdjustRoomCapacityUseCase {

  private final HotelRepository hotelRepository;
  private final RoomAvailabilityRepository roomAvailabilityRepository;

  public void execute(RoomAvailabilityPeriodCommand command) {
    Hotel hotel =
        hotelRepository
            .findById(command.hotelId())
            .orElseThrow(() -> new HotelNotFoundException(command.hotelId()));

    hotel.requireRoomType(command.roomTypeId());

    validateRangeExists(
        command.hotelId(), command.roomTypeId(), command.startDate(), command.endDate());
    adjustRange(
        command.hotelId(),
        command.roomTypeId(),
        command.startDate(),
        command.endDate(),
        command.totalRooms());
  }

  private void validateRangeExists(
      UUID hotelId, UUID roomTypeId, LocalDate startDate, LocalDate endDate) {

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      if (roomAvailabilityRepository
          .findByHotelIdAndRoomTypeIdAndDate(hotelId, roomTypeId, date)
          .isEmpty()) {
        throw new RoomAvailabilityNotFoundException(hotelId, roomTypeId, date);
      }
    }
  }

  private void adjustRange(
      UUID hotelId, UUID roomTypeId, LocalDate startDate, LocalDate endDate, int totalRooms) {

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      final LocalDate finalDate = date;
      RoomAvailability roomAvailability =
          roomAvailabilityRepository
              .findByHotelIdAndRoomTypeIdAndDate(hotelId, roomTypeId, date)
              .orElseThrow(
                  () -> new RoomAvailabilityNotFoundException(hotelId, roomTypeId, finalDate));

      roomAvailability.adjustCapacity(totalRooms);
      roomAvailabilityRepository.save(roomAvailability);
    }
  }
}
