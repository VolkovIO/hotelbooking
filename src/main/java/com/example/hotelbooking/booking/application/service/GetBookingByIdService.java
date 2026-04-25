package com.example.hotelbooking.booking.application.service;

import com.example.hotelbooking.booking.application.exception.BookingNotFoundException;
import com.example.hotelbooking.booking.application.port.in.GetBookingByIdUseCase;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetBookingByIdService implements GetBookingByIdUseCase {

  private final BookingRepository bookingRepository;

  @Override
  public Booking execute(BookingId bookingId) {
    return bookingRepository
        .findById(bookingId)
        .orElseThrow(() -> new BookingNotFoundException(bookingId));
  }
}
