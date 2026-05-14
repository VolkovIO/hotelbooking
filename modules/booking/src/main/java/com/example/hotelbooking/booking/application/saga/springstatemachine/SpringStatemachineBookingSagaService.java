package com.example.hotelbooking.booking.application.saga.springstatemachine;

import static com.example.hotelbooking.booking.application.saga.springstatemachine.SpringStatemachineBookingSagaEvent.NEXT;

import com.example.hotelbooking.booking.application.port.out.BookingMetrics;
import com.example.hotelbooking.booking.application.port.out.BookingObservabilityContext;
import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaFailureReason;
import com.example.hotelbooking.booking.application.saga.BookingSagaId;
import com.example.hotelbooking.booking.application.saga.BookingSagaNotFoundException;
import com.example.hotelbooking.booking.application.saga.BookingSagaStatus;
import com.example.hotelbooking.booking.application.saga.BookingSagaStep;
import java.util.Locale;
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

  private static final int MAX_TRANSITIONS_PER_RUN = 20;
  private static final String IMPLEMENTATION = "spring-statemachine";

  private final BookingSagaRepository sagaRepository;
  private final StateMachineFactory<
          SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
      stateMachineFactory;
  private final BookingObservabilityContext observabilityContext;
  private final BookingMetrics bookingMetrics;

  public BookingSaga process(BookingSagaId sagaId) {
    Objects.requireNonNull(sagaId, "sagaId must not be null");

    BookingSaga saga =
        sagaRepository.findById(sagaId).orElseThrow(() -> new BookingSagaNotFoundException(sagaId));

    try (BookingObservabilityContext.ContextScope ignored = observabilityContext.openSaga(saga)) {
      return processWithContext(saga);
    }
  }

  private BookingSaga processWithContext(BookingSaga saga) {
    StateMachine<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
        stateMachine = stateMachineFactory.getStateMachine(saga.getId().value().toString());

    stateMachine
        .getExtendedState()
        .getVariables()
        .put(SpringStatemachineBookingSagaContext.KEY, saga);

    stateMachine.startReactively().block();

    int executedTransitions = 0;

    while (!saga.isFinished() && saga.getCurrentStep() != BookingSagaStep.COMPLETE) {
      if (executedTransitions >= MAX_TRANSITIONS_PER_RUN) {
        markFailed(
            saga,
            "Spring Statemachine prototype exceeded maximum transitions per run: sagaId="
                + saga.getId().value());

        break;
      }

      SpringStatemachineBookingSagaState stateBefore = stateMachine.getState().getId();
      BookingSagaStep stepBefore = saga.getCurrentStep();
      BookingSagaStatus statusBefore = saga.getStatus();

      stateMachine
          .sendEvent(
              Mono.just(
                  MessageBuilder.withPayload(NEXT)
                      .setHeader(SpringStatemachineBookingSagaContext.KEY, saga)
                      .build()))
          .blockLast();

      if (!saga.isFinished()
          && stateMachine.getState().getId() == stateBefore
          && saga.getCurrentStep() == stepBefore
          && saga.getStatus() == statusBefore) {
        markFailed(
            saga,
            "Spring Statemachine prototype did not make progress: sagaId="
                + saga.getId().value()
                + ", state="
                + stateBefore
                + ", step="
                + stepBefore);

        break;
      }

      executedTransitions++;
    }

    stateMachine.stopReactively().block();

    log.info(
        "Spring Statemachine booking saga prototype finished: sagaId={}, bookingId={}, status={}, currentStep={}",
        saga.getId().value(),
        saga.getBookingId(),
        saga.getStatus(),
        saga.getCurrentStep());

    bookingMetrics.sagaProcessed(IMPLEMENTATION, outcome(saga));

    return saga;
  }

  private void markFailed(BookingSaga saga, String reason) {
    saga.markFailed(new BookingSagaFailureReason(reason));
    sagaRepository.save(saga);

    log.warn(
        "Spring Statemachine booking saga prototype failed: sagaId={}, bookingId={}, currentStep={}, reason={}",
        saga.getId().value(),
        saga.getBookingId(),
        saga.getCurrentStep(),
        reason);
  }

  private String outcome(BookingSaga saga) {
    return saga.getStatus().name().toLowerCase(Locale.ROOT);
  }
}
