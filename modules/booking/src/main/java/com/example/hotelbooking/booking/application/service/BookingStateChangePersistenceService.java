package com.example.hotelbooking.booking.application.service;

import com.example.hotelbooking.booking.application.event.BookingLifecycleEvent;
import com.example.hotelbooking.booking.application.event.BookingOutboxMessage;
import com.example.hotelbooking.booking.application.port.out.BookingOutboxRepository;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.domain.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingStateChangePersistenceService {

  private final BookingRepository bookingRepository;
  private final BookingOutboxRepository bookingOutboxRepository;

  @Transactional
  public Booking persist(Booking booking, BookingLifecycleEvent event) {
    // The aggregate update and the outbox insert must be committed atomically.
    Booking savedBooking = bookingRepository.save(booking);

    bookingOutboxRepository.save(BookingOutboxMessage.from(event));

    return savedBooking;
  }
}
