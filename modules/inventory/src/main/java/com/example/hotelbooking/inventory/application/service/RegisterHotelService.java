package com.example.hotelbooking.inventory.application.service;

import com.example.hotelbooking.inventory.application.command.RegisterHotelCommand;
import com.example.hotelbooking.inventory.application.port.in.RegisterHotelUseCase;
import com.example.hotelbooking.inventory.application.port.out.HotelRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterHotelService implements RegisterHotelUseCase {

  private final HotelRepository hotelRepository;

  @Override
  public Hotel execute(RegisterHotelCommand command) {
    log.info("Registering hotel: name={}, city={}", command.name(), command.city());

    Hotel hotel = Hotel.create(command.name(), command.city());

    Hotel saveHotel = hotelRepository.save(hotel);

    log.info(
        "Hotel registered: hotelId={}, name={}, city={}",
        saveHotel.getId(),
        saveHotel.getName(),
        saveHotel.getCity());

    return saveHotel;
  }
}
