package com.example.hotelbooking.booking.application.port.in;

import com.example.hotelbooking.booking.application.query.GetBookingByIdQuery;
import com.example.hotelbooking.booking.domain.Booking;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface GetBookingByIdUseCase {
  Booking execute(GetBookingByIdQuery query);
}
