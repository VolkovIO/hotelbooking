package com.example.hotelbooking.booking.application.saga.action;

import com.example.hotelbooking.booking.application.exception.BookingNotFoundException;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class BookingSagaBookingLoader {

  private final BookingRepository bookingRepository;

  Booking load(BookingSaga saga) {
    BookingId bookingId = new BookingId(saga.getBookingId());

    return bookingRepository
        .findById(bookingId)
        .orElseThrow(() -> new BookingNotFoundException(bookingId));
  }
}
