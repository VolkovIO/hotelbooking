package com.example.hotelbooking.booking.application.saga.springstatemachine;

import com.example.hotelbooking.booking.application.command.StartBookingSagaCommand;
import com.example.hotelbooking.booking.application.saga.BookingSaga;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface StartSpringStatemachineBookingSagaUseCase {

  BookingSaga execute(StartBookingSagaCommand command);
}
