package com.example.hotelbooking.inventory.adapter.out.persistence;

import com.example.hotelbooking.inventory.application.port.out.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.domain.InventoryDomainException;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("inventory-in-memory")
class InMemoryRoomAvailabilityRepository implements RoomAvailabilityRepository {

  private final Map<String, RoomAvailability> storage = new ConcurrentHashMap<>();

  @Override
  public synchronized void saveAll(List<RoomAvailability> availabilityList) {
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

  @Override
  public Optional<RoomAvailability> findByHotelIdAndRoomTypeIdAndDate(
      UUID hotelId, UUID roomTypeId, LocalDate date) {
    return Optional.ofNullable(storage.get(keyOf(hotelId, roomTypeId, date)));
  }

  @Override
  public synchronized RoomAvailability save(RoomAvailability availability) {
    storage.put(
        keyOf(availability.getHotelId(), availability.getRoomTypeId(), availability.getDate()),
        availability);
    return availability;
  }

  @Override
  public synchronized boolean tryPlaceHold(
      UUID hotelId, UUID roomTypeId, LocalDate date, int rooms) {
    String key = keyOf(hotelId, roomTypeId, date);
    RoomAvailability current = storage.get(key);

    if (current == null) {
      return false;
    }

    try {
      storage.put(key, current.placeHold(rooms));
      return true;
    } catch (InventoryDomainException exception) {
      return false;
    }
  }

  @Override
  public synchronized boolean releaseHold(
      UUID hotelId, UUID roomTypeId, LocalDate date, int rooms) {
    String key = keyOf(hotelId, roomTypeId, date);
    RoomAvailability current = storage.get(key);

    if (current == null) {
      return false;
    }

    try {
      storage.put(key, current.releaseHold(rooms));
      return true;
    } catch (InventoryDomainException exception) {
      return false;
    }
  }

  private String keyOf(UUID hotelId, UUID roomTypeId, LocalDate date) {
    return hotelId + ":" + roomTypeId + ":" + date;
  }
}
