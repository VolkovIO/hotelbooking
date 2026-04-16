package com.example.hotelbooking.inventory.application.command;

import com.example.hotelbooking.inventory.application.port.HotelRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterHotelUseCase {

  private final HotelRepository hotelRepository;

  public Hotel execute(RegisterHotelCommand command) {
    Hotel hotel = Hotel.create(command.name(), command.city());
    return hotelRepository.save(hotel);
  }
}
