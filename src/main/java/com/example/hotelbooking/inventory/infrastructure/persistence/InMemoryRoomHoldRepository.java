package com.example.hotelbooking.inventory.infrastructure.persistence;

import com.example.hotelbooking.inventory.application.port.RoomHoldRepository;
import com.example.hotelbooking.inventory.domain.RoomHold;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("in-memory")
public class InMemoryRoomHoldRepository implements RoomHoldRepository {

  private final Map<UUID, RoomHold> storage = new ConcurrentHashMap<>();

  @Override
  public RoomHold save(RoomHold roomHold) {
    storage.put(roomHold.getId(), roomHold);
    return roomHold;
  }
}
