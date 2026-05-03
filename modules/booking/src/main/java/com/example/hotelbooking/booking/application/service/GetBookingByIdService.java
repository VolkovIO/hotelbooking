package com.example.hotelbooking.booking.application.service;

import com.example.hotelbooking.booking.application.exception.BookingNotFoundException;
import com.example.hotelbooking.booking.application.port.in.GetBookingByIdUseCase;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.query.GetBookingByIdQuery;
import com.example.hotelbooking.booking.domain.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetBookingByIdService implements GetBookingByIdUseCase {

  private final BookingRepository bookingRepository;

  @Override
  public Booking execute(GetBookingByIdQuery query) {
    log.debug("Getting booking by id: bookingId={}", query.bookingId());

    Booking booking =
        bookingRepository
            .findById(query.bookingId())
            .orElseThrow(() -> new BookingNotFoundException(query.bookingId()));

    log.debug("Booking found: bookingId={}, status={}", booking.getId(), booking.getStatus());

    return booking;
  }
}
