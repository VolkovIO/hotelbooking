package com.example.hotelbooking.booking.application.port.out;

import com.example.hotelbooking.booking.application.event.BookingOutboxMessage;

public interface BookingOutboxRepository {

  void save(BookingOutboxMessage message);
}
