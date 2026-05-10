package com.example.hotelbooking.booking.application.saga.springstatemachine;

import com.example.hotelbooking.booking.application.command.StartBookingSagaCommand;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaStartPersistenceService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("booking-saga-springstatemachine-prototype")
@RequiredArgsConstructor
public class StartSpringStatemachineBookingSagaService
    implements StartSpringStatemachineBookingSagaUseCase {

  private final BookingSagaStartPersistenceService startPersistenceService;
  private final SpringStatemachineBookingSagaService springStatemachineSagaService;

  @Override
  public BookingSaga execute(StartBookingSagaCommand command) {
    Objects.requireNonNull(command, "command must not be null");

    BookingSaga saga = startPersistenceService.createBookingAndSaga(command);

    log.info(
        "Spring Statemachine booking saga prototype started: sagaId={}, bookingId={}",
        saga.getId().value(),
        saga.getBookingId());

    return springStatemachineSagaService.process(saga.getId());
  }
}
