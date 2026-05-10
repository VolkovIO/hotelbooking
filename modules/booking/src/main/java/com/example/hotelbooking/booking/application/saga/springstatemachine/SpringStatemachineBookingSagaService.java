package com.example.hotelbooking.booking.application.saga.springstatemachine;

import static com.example.hotelbooking.booking.application.saga.springstatemachine.SpringStatemachineBookingSagaEvent.NEXT;

import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaId;
import com.example.hotelbooking.booking.application.saga.BookingSagaNotFoundException;
import com.example.hotelbooking.booking.application.saga.BookingSagaStep;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@Profile("booking-saga-springstatemachine-prototype")
@RequiredArgsConstructor
public class SpringStatemachineBookingSagaService {

  private final BookingSagaRepository sagaRepository;
  private final StateMachineFactory<
          SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
      stateMachineFactory;

  public BookingSaga process(BookingSagaId sagaId) {
    Objects.requireNonNull(sagaId, "sagaId must not be null");

    BookingSaga saga =
        sagaRepository.findById(sagaId).orElseThrow(() -> new BookingSagaNotFoundException(sagaId));

    StateMachine<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
        stateMachine = stateMachineFactory.getStateMachine(saga.getId().value().toString());

    stateMachine
        .getExtendedState()
        .getVariables()
        .put(SpringStatemachineBookingSagaContext.KEY, saga);

    stateMachine.startReactively().block();

    while (!saga.isFinished() && saga.getCurrentStep() != BookingSagaStep.COMPLETE) {

      stateMachine
          .sendEvent(
              Mono.just(
                  MessageBuilder.withPayload(NEXT)
                      .setHeader(SpringStatemachineBookingSagaContext.KEY, saga)
                      .build()))
          .blockLast();
    }

    stateMachine.stopReactively().block();

    log.info(
        "Spring Statemachine booking saga prototype finished: sagaId={}, bookingId={}, status={}, currentStep={}",
        saga.getId().value(),
        saga.getBookingId(),
        saga.getStatus(),
        saga.getCurrentStep());

    return saga;
  }
}
