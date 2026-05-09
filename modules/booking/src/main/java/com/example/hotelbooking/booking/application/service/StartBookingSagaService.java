package com.example.hotelbooking.booking.application.service;

import com.example.hotelbooking.booking.application.command.StartBookingSagaCommand;
import com.example.hotelbooking.booking.application.port.in.StartBookingSagaUseCase;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaProcessManager;
import com.example.hotelbooking.booking.application.saga.BookingSagaStartPersistenceService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Starts a booking saga from the application layer.
 *
 * <p>This service intentionally has no @Transactional annotation. The initial local state is saved
 * transactionally by BookingSagaStartPersistenceService. After that, BookingSagaProcessManager
 * calls external services and persists progress step by step.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StartBookingSagaService implements StartBookingSagaUseCase {

  private final BookingSagaStartPersistenceService startPersistenceService;
  private final BookingSagaProcessManager processManager;

  @Override
  public BookingSaga execute(StartBookingSagaCommand command) {
    Objects.requireNonNull(command, "command must not be null");

    BookingSaga saga = startPersistenceService.createBookingAndSaga(command);

    log.info(
        "Booking saga started: sagaId={}, bookingId={}", saga.getId().value(), saga.getBookingId());

    return processManager.process(saga.getId());
  }
}
