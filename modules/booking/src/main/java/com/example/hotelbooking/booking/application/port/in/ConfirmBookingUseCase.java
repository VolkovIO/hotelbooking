package com.example.hotelbooking.booking.application.port.in;

import com.example.hotelbooking.booking.application.command.ConfirmBookingCommand;
import com.example.hotelbooking.booking.domain.Booking;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ConfirmBookingUseCase {
  Booking execute(ConfirmBookingCommand command);
}
