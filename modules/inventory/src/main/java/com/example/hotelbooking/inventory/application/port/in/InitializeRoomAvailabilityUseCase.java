package com.example.hotelbooking.inventory.application.port.in;

import com.example.hotelbooking.inventory.application.command.RoomAvailabilityPeriodCommand;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface InitializeRoomAvailabilityUseCase {
  void execute(RoomAvailabilityPeriodCommand command);
}
