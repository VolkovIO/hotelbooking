package com.example.hotelbooking.inventory.application.port.in;

import com.example.hotelbooking.inventory.application.command.AddRoomTypeCommand;
import com.example.hotelbooking.inventory.domain.Hotel;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface AddRoomTypeUseCase {
  Hotel execute(AddRoomTypeCommand command);
}
