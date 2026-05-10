package com.example.hotelbooking.booking.application.saga.action;

import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaStep;

public interface BookingSagaAction {

  BookingSagaStep step();

  BookingSaga execute(BookingSaga saga);
}
