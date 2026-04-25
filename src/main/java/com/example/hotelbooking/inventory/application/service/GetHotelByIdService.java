package com.example.hotelbooking.inventory.application.service;

import com.example.hotelbooking.inventory.application.exception.HotelNotFoundException;
import com.example.hotelbooking.inventory.application.port.in.GetHotelByIdUseCase;
import com.example.hotelbooking.inventory.application.port.out.HotelRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetHotelByIdService implements GetHotelByIdUseCase {

  private final HotelRepository hotelRepository;

  @Override
  public Hotel execute(UUID hotelId) {
    return hotelRepository.findById(hotelId).orElseThrow(() -> new HotelNotFoundException(hotelId));
  }
}
