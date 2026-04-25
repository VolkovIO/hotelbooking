package com.example.hotelbooking.booking.application.port.in;

import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface GetBookingByIdUseCase {
  Booking execute(BookingId bookingId);
}
