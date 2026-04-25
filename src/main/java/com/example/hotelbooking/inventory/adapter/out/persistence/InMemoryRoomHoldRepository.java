package com.example.hotelbooking.inventory.adapter.out.persistence;

import com.example.hotelbooking.inventory.application.port.out.RoomHoldRepository;
import com.example.hotelbooking.inventory.domain.RoomHold;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("in-memory")
final class InMemoryRoomHoldRepository implements RoomHoldRepository {

  private final Map<UUID, RoomHold> storage = new ConcurrentHashMap<>();

  @Override
  public RoomHold save(RoomHold roomHold) {
    storage.put(roomHold.getId(), roomHold);
    return roomHold;
  }

  @Override
  public Optional<RoomHold> findById(UUID holdId) {
    return Optional.ofNullable(storage.get(holdId));
  }

  @Override
  public void deleteById(UUID holdId) {
    storage.remove(holdId);
  }
}
