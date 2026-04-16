package com.example.hotelbooking.inventory.infrastructure.persistence;

import com.example.hotelbooking.inventory.application.port.HotelRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("in-memory")
public class InMemoryHotelRepository implements HotelRepository {

  private final Map<UUID, Hotel> storage = new ConcurrentHashMap<>();

  @Override
  public Hotel save(Hotel hotel) {
    storage.put(hotel.getId(), hotel);
    return hotel;
  }

  @Override
  public Optional<Hotel> findById(UUID hotelId) {
    return Optional.ofNullable(storage.get(hotelId));
  }
}
