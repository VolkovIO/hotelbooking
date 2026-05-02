package com.example.hotelbooking.inventory.application.service;

import com.example.hotelbooking.inventory.application.command.AddRoomTypeCommand;
import com.example.hotelbooking.inventory.application.exception.HotelNotFoundException;
import com.example.hotelbooking.inventory.application.port.in.AddRoomTypeUseCase;
import com.example.hotelbooking.inventory.application.port.out.HotelRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddRoomTypeService implements AddRoomTypeUseCase {

  private final HotelRepository hotelRepository;

  @Override
  public Hotel execute(AddRoomTypeCommand command) {
    log.info(
        "Adding room type: hotelId={}, roomTypeName={}, guestCapacity={}",
        command.hotelId(),
        command.name(),
        command.guestCapacity());

    Hotel hotel =
        hotelRepository
            .findById(command.hotelId())
            .orElseThrow(() -> new HotelNotFoundException(command.hotelId()));

    hotel.addRoomType(command.name(), command.guestCapacity());

    Hotel saveHotel = hotelRepository.save(hotel);

    log.info(
        "Room type added: hotelId={}, roomTypeName={}, guestCapacity={}",
        saveHotel.getId(),
        command.name(),
        command.guestCapacity());

    return saveHotel;
  }
}
