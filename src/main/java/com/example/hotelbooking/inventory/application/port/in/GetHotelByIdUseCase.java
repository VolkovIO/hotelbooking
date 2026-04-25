package com.example.hotelbooking.inventory.application.port.in;

import com.example.hotelbooking.inventory.domain.Hotel;
import java.util.UUID;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface GetHotelByIdUseCase {
  Hotel execute(UUID hotelId);
}
