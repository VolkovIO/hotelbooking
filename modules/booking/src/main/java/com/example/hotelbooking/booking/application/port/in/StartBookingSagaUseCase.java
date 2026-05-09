package com.example.hotelbooking.booking.application.port.in;

import com.example.hotelbooking.booking.application.command.StartBookingSagaCommand;
import com.example.hotelbooking.booking.application.saga.BookingSaga;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface StartBookingSagaUseCase {

  BookingSaga execute(StartBookingSagaCommand command);
}
