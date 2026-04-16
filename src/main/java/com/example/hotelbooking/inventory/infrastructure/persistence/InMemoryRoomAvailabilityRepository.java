package com.example.hotelbooking.inventory.infrastructure.persistence;

import com.example.hotelbooking.inventory.application.port.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("in-memory")
public class InMemoryRoomAvailabilityRepository implements RoomAvailabilityRepository {

  private final Map<String, RoomAvailability> storage = new ConcurrentHashMap<>();

  @Override
  public void saveAll(List<RoomAvailability> availabilityList) {
    for (RoomAvailability availability : availabilityList) {
      storage.put(
          keyOf(availability.getHotelId(), availability.getRoomTypeId(), availability.getDate()),
          availability);
    }
  }

  @Override
  public List<RoomAvailability> findByRoomTypeAndDateRange(
      UUID hotelId, UUID roomTypeId, LocalDate from, LocalDate to) {
    return storage.values().stream()
        .filter(item -> item.getHotelId().equals(hotelId))
        .filter(item -> item.getRoomTypeId().equals(roomTypeId))
        .filter(item -> !item.getDate().isBefore(from) && !item.getDate().isAfter(to))
        .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
        .toList();
  }

  private String keyOf(UUID hotelId, UUID roomTypeId, LocalDate date) {
    return hotelId + ":" + roomTypeId + ":" + date;
  }
}
