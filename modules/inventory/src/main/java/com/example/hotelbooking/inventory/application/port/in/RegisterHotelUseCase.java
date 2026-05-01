package com.example.hotelbooking.inventory.application.port.in;

import com.example.hotelbooking.inventory.application.command.RegisterHotelCommand;
import com.example.hotelbooking.inventory.domain.Hotel;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface RegisterHotelUseCase {
  Hotel execute(RegisterHotelCommand command);
}
