package com.example.hotelbooking.booking.application.port.in;

import com.example.hotelbooking.booking.application.command.CancelBookingCommand;
import com.example.hotelbooking.booking.domain.Booking;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface CancelBookingUseCase {
  Booking execute(CancelBookingCommand command);
}
