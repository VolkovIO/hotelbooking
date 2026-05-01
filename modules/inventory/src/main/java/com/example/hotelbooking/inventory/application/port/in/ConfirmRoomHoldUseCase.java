package com.example.hotelbooking.inventory.application.port.in;

import java.util.UUID;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ConfirmRoomHoldUseCase {
  void execute(UUID holdId);
}
