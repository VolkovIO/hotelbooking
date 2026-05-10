package com.example.hotelbooking.booking.application.saga.springstatemachine;

import com.example.hotelbooking.booking.application.saga.BookingSaga;
import org.springframework.statemachine.StateContext;

final class SpringStatemachineBookingSagaContext {

  static final String KEY = "bookingSaga";

  private SpringStatemachineBookingSagaContext() {}

  static BookingSaga get(
      StateContext<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
          context) {
    return context.getExtendedState().get(KEY, BookingSaga.class);
  }
}
