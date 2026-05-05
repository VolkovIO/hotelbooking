package com.example.hotelbooking.booking.application.port.out;

import com.example.hotelbooking.booking.application.event.BookingOutboxMessage;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface BookingOutboxEventPublisher {

  void publish(BookingOutboxMessage message);
}
