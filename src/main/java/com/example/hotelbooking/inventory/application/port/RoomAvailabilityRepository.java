package com.example.hotelbooking.inventory.application.port;

import com.example.hotelbooking.inventory.domain.RoomAvailability;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomAvailabilityRepository {

  void saveAll(List<RoomAvailability> availabilityList);

  List<RoomAvailability> findByRoomTypeAndDateRange(
      UUID hotelId, UUID roomTypeId, LocalDate from, LocalDate to);

  Optional<RoomAvailability> findByHotelIdAndRoomTypeIdAndDate(
      UUID hotelId, UUID roomTypeId, LocalDate date);

  RoomAvailability save(RoomAvailability availability);
}
