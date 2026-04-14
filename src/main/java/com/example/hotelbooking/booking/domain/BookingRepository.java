package com.example.hotelbooking.booking.domain;

import java.util.Optional;

public interface BookingRepository {

  Booking save(Booking booking);

  Optional<Booking> findById(BookingId bookingId);
}
