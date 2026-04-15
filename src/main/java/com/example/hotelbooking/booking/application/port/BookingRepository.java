package com.example.hotelbooking.booking.application.port;

import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;
import java.util.Optional;

public interface BookingRepository {

  Booking save(Booking booking);

  Optional<Booking> findById(BookingId bookingId);
}
