package com.example.hotelbooking.booking.application.port.in;

import com.example.hotelbooking.booking.application.command.CreateBookingCommand;
import com.example.hotelbooking.booking.domain.Booking;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface CreateBookingUseCase {
  Booking execute(CreateBookingCommand command);
}
