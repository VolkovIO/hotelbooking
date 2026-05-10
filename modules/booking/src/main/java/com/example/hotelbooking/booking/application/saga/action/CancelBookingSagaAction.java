package com.example.hotelbooking.booking.application.saga.action;

import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancelBookingSagaAction implements BookingSagaAction {

  private final BookingSagaRepository sagaRepository;

  @Override
  public BookingSagaStep step() {
    return BookingSagaStep.CANCEL_BOOKING;
  }

  @Override
  public BookingSaga execute(BookingSaga saga) {
    saga.markBookingCancelled();
    sagaRepository.save(saga);

    log.info(
        "Booking saga compensation completed: sagaId={}, bookingId={}",
        saga.getId().value(),
        saga.getBookingId());

    return saga;
  }
}
