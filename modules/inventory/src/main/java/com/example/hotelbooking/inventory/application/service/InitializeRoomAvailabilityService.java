package com.example.hotelbooking.inventory.application.service;

import com.example.hotelbooking.inventory.application.command.RoomAvailabilityPeriodCommand;
import com.example.hotelbooking.inventory.application.exception.HotelNotFoundException;
import com.example.hotelbooking.inventory.application.exception.RoomAvailabilityAlreadyExistsException;
import com.example.hotelbooking.inventory.application.port.in.InitializeRoomAvailabilityUseCase;
import com.example.hotelbooking.inventory.application.port.out.HotelRepository;
import com.example.hotelbooking.inventory.application.port.out.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InitializeRoomAvailabilityService implements InitializeRoomAvailabilityUseCase {

  private final HotelRepository hotelRepository;
  private final RoomAvailabilityRepository roomAvailabilityRepository;

  @Override
  public void execute(RoomAvailabilityPeriodCommand command) {
    log.info(
        "Initializing room availability: hotelId={}, roomTypeId={}, startDate={}, endDate={}, totalRooms={}",
        command.hotelId(),
        command.roomTypeId(),
        command.startDate(),
        command.endDate(),
        command.totalRooms());

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

    log.info(
        "Room availability initialized: hotelId={}, roomTypeId={}, startDate={}, endDate={}, totalRooms={}",
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
