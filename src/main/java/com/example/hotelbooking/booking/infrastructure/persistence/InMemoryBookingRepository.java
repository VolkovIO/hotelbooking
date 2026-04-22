package com.example.hotelbooking.booking.infrastructure.persistence;

import com.example.hotelbooking.booking.application.port.BookingRepository;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("in-memory")
final class InMemoryBookingRepository implements BookingRepository {

  private final Map<BookingId, Booking> storage = new ConcurrentHashMap<>();

  @Override
  public Booking save(Booking booking) {
    storage.put(booking.getId(), booking);
    return booking;
  }

  @Override
  public Optional<Booking> findById(BookingId bookingId) {
    return Optional.ofNullable(storage.get(bookingId));
  }
}
