package com.example.hotelbooking.booking.application;

import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;
import com.example.hotelbooking.booking.domain.BookingNotFoundException;
import com.example.hotelbooking.booking.domain.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetBookingByIdUseCase {

  private final BookingRepository bookingRepository;

  public Booking execute(BookingId bookingId) {
    return bookingRepository
        .findById(bookingId)
        .orElseThrow(() -> new BookingNotFoundException(bookingId));
  }
}
