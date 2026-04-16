package com.example.hotelbooking.inventory.application.command;

import com.example.hotelbooking.inventory.application.exception.HotelNotFoundException;
import com.example.hotelbooking.inventory.application.port.HotelRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AddRoomTypeUseCase {

  private final HotelRepository hotelRepository;

  public Hotel execute(AddRoomTypeCommand command) {
    Hotel hotel =
        hotelRepository
            .findById(command.hotelId())
            .orElseThrow(() -> new HotelNotFoundException(command.hotelId()));

    hotel.addRoomType(command.name(), command.guestCapacity());

    return hotelRepository.save(hotel);
  }
}
