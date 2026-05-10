package com.example.hotelbooking.booking.application.saga.springstatemachine;

import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaStep;
import com.example.hotelbooking.booking.application.saga.action.BookingSagaActionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

@Component
@Profile("booking-saga-springstatemachine-prototype")
@RequiredArgsConstructor
class SpringStatemachineBookingSagaActions {

  private final BookingSagaActionRegistry actionRegistry;

  Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent> holdInventory() {
    return executeAction();
  }

  Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
      authorizePayment() {
    return executeAction();
  }

  Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent> confirmBooking() {
    return executeAction();
  }

  Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent> approvePayment() {
    return executeAction();
  }

  Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent> cancelPayment() {
    return executeAction();
  }

  Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
      releaseInventory() {
    return executeAction();
  }

  Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent> cancelBooking() {
    return executeAction();
  }

  Guard<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
      paymentAuthorized() {
    return context ->
        SpringStatemachineBookingSagaContext.get(context).getCurrentStep()
            == BookingSagaStep.CONFIRM_BOOKING;
  }

  Guard<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent> paymentDeclined() {
    return context ->
        SpringStatemachineBookingSagaContext.get(context).getCurrentStep()
            == BookingSagaStep.RELEASE_INVENTORY;
  }

  private Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
      executeAction() {
    return context -> {
      BookingSaga saga = SpringStatemachineBookingSagaContext.get(context);
      actionRegistry.execute(saga);
    };
  }
}
