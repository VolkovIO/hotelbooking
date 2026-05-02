package com.example.hotelbooking.inventory.application.port.out;

import com.example.hotelbooking.inventory.domain.RoomHold;
import java.util.Optional;
import java.util.UUID;

public interface RoomHoldRepository {

  RoomHold save(RoomHold roomHold);

  Optional<RoomHold> findById(UUID holdId);

  void deleteById(UUID holdId);
}
