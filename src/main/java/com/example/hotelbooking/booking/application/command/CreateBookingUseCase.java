package com.example.hotelbooking.booking.application.command;

import com.example.hotelbooking.booking.application.port.BookingRepository;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.StayPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateBookingUseCase {

  private final BookingRepository bookingRepository;

  public Booking execute(CreateBookingCommand command) {
    StayPeriod stayPeriod = new StayPeriod(command.checkIn(), command.checkOut());

    Booking booking =
        Booking.create(command.hotelId(), command.roomTypeId(), stayPeriod, command.guestCount());

    return bookingRepository.save(booking);
  }
}
