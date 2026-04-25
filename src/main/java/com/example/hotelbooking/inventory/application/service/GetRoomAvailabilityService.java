package com.example.hotelbooking.inventory.application.service;

import com.example.hotelbooking.inventory.application.port.in.GetRoomAvailabilityUseCase;
import com.example.hotelbooking.inventory.application.port.out.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetRoomAvailabilityService implements GetRoomAvailabilityUseCase {

  private final RoomAvailabilityRepository roomAvailabilityRepository;

  @Override
  public List<RoomAvailability> execute(
      UUID hotelId, UUID roomTypeId, LocalDate from, LocalDate to) {
    return roomAvailabilityRepository.findByRoomTypeAndDateRange(hotelId, roomTypeId, from, to);
  }
}
