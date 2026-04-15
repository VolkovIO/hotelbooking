package com.example.hotelbooking.inventory.application.port;

import com.example.hotelbooking.inventory.domain.Hotel;
import java.util.Optional;
import java.util.UUID;

public interface HotelRepository {

  Hotel save(Hotel hotel);

  Optional<Hotel> findById(UUID hotelId);
}
