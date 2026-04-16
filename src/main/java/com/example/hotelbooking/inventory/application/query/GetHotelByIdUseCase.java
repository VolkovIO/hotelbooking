package com.example.hotelbooking.inventory.application.query;

import com.example.hotelbooking.inventory.application.exception.HotelNotFoundException;
import com.example.hotelbooking.inventory.application.port.HotelRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetHotelByIdUseCase {

  private final HotelRepository hotelRepository;

  public Hotel execute(UUID hotelId) {
    return hotelRepository.findById(hotelId).orElseThrow(() -> new HotelNotFoundException(hotelId));
  }
}
