package com.example.hotelbooking.inventory.application.service;

import com.example.hotelbooking.inventory.application.command.RegisterHotelCommand;
import com.example.hotelbooking.inventory.application.port.in.RegisterHotelUseCase;
import com.example.hotelbooking.inventory.application.port.out.HotelRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterHotelService implements RegisterHotelUseCase {

  private final HotelRepository hotelRepository;

  @Override
  public Hotel execute(RegisterHotelCommand command) {
    Hotel hotel = Hotel.create(command.name(), command.city());
    return hotelRepository.save(hotel);
  }
}
