package com.example.hotelbooking.inventory.application.port;

import com.example.hotelbooking.inventory.domain.RoomHold;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface RoomHoldRepository {

  RoomHold save(RoomHold roomHold);
}
