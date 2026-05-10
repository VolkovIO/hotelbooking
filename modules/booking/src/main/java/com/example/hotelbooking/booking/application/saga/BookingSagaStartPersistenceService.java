package com.example.hotelbooking.booking.application.saga;

import com.example.hotelbooking.booking.application.command.StartBookingSagaCommand;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import com.example.hotelbooking.booking.domain.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists the initial local state required to start a booking saga.
 *
 * <p>This service has a deliberately small transaction boundary. It only stores local PostgreSQL
 * state: Booking and BookingSaga. It does not call inventory-service or payment-service.
 *
 * <p>The external distributed steps are executed later by BookingSagaProcessManager, outside of
 * this transaction.
 */
@Service
@RequiredArgsConstructor
public class BookingSagaStartPersistenceService {

  private final BookingRepository bookingRepository;
  private final BookingSagaRepository sagaRepository;

  @Transactional
  public BookingSaga createBookingAndSaga(StartBookingSagaCommand command) {
    Booking booking =
        Booking.create(
            command.userId(),
            command.hotelId(),
            command.roomTypeId(),
            command.stayPeriod(),
            command.guestCount());

    Booking savedBooking = bookingRepository.save(booking);

    BookingSaga saga =
        BookingSaga.start(
            savedBooking.getId().value(), command.paymentAmount(), command.paymentCurrency());

    return sagaRepository.save(saga);
  }
}
