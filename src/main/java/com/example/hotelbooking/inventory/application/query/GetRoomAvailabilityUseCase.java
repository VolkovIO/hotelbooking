package com.example.hotelbooking.inventory.application.query;

import com.example.hotelbooking.inventory.application.port.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetRoomAvailabilityUseCase {

  private final RoomAvailabilityRepository roomAvailabilityRepository;

  public List<RoomAvailability> execute(
      UUID hotelId, UUID roomTypeId, LocalDate from, LocalDate to) {
    return roomAvailabilityRepository.findByRoomTypeAndDateRange(hotelId, roomTypeId, from, to);
  }
}
