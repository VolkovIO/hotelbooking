package com.example.hotelbooking.inventory.application.command;

import com.example.hotelbooking.inventory.application.exception.HotelNotFoundException;
import com.example.hotelbooking.inventory.application.exception.RoomAvailabilityNotFoundException;
import com.example.hotelbooking.inventory.application.port.HotelRepository;
import com.example.hotelbooking.inventory.application.port.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import java.time.LocalDate;
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

    for (LocalDate date = command.startDate();
        !date.isAfter(command.endDate());
        date = date.plusDays(1)) {

      final LocalDate currentDate = date;

      RoomAvailability availability =
          roomAvailabilityRepository
              .findByHotelIdAndRoomTypeIdAndDate(
                  command.hotelId(), command.roomTypeId(), currentDate)
              .orElseThrow(
                  () ->
                      new RoomAvailabilityNotFoundException(
                          command.hotelId(), command.roomTypeId(), currentDate));

      availability.adjustCapacity(command.totalRooms());
      roomAvailabilityRepository.save(availability);
    }
  }
}
