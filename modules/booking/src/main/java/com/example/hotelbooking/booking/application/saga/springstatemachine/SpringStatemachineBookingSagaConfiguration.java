package com.example.hotelbooking.booking.application.saga.springstatemachine;

import static com.example.hotelbooking.booking.application.saga.springstatemachine.SpringStatemachineBookingSagaEvent.FAIL;
import static com.example.hotelbooking.booking.application.saga.springstatemachine.SpringStatemachineBookingSagaEvent.NEXT;
import static com.example.hotelbooking.booking.application.saga.springstatemachine.SpringStatemachineBookingSagaState.APPROVE_PAYMENT;
import static com.example.hotelbooking.booking.application.saga.springstatemachine.SpringStatemachineBookingSagaState.AUTHORIZE_PAYMENT;
import static com.example.hotelbooking.booking.application.saga.springstatemachine.SpringStatemachineBookingSagaState.CANCEL_BOOKING;
import static com.example.hotelbooking.booking.application.saga.springstatemachine.SpringStatemachineBookingSagaState.CANCEL_PAYMENT;
import static com.example.hotelbooking.booking.application.saga.springstatemachine.SpringStatemachineBookingSagaState.COMPENSATED;
import static com.example.hotelbooking.booking.application.saga.springstatemachine.SpringStatemachineBookingSagaState.COMPLETED;
import static com.example.hotelbooking.booking.application.saga.springstatemachine.SpringStatemachineBookingSagaState.CONFIRM_BOOKING;
import static com.example.hotelbooking.booking.application.saga.springstatemachine.SpringStatemachineBookingSagaState.FAILED;
import static com.example.hotelbooking.booking.application.saga.springstatemachine.SpringStatemachineBookingSagaState.HOLD_INVENTORY;
import static com.example.hotelbooking.booking.application.saga.springstatemachine.SpringStatemachineBookingSagaState.PAYMENT_AUTHORIZATION_DECISION;
import static com.example.hotelbooking.booking.application.saga.springstatemachine.SpringStatemachineBookingSagaState.RELEASE_INVENTORY;

import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

@Configuration
@Profile("booking-saga-springstatemachine-prototype")
@EnableStateMachineFactory
@RequiredArgsConstructor
class SpringStatemachineBookingSagaConfiguration
    extends EnumStateMachineConfigurerAdapter<
        SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent> {

  private final SpringStatemachineBookingSagaActions actions;

  @Override
  public void configure(
      StateMachineStateConfigurer<
              SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
          states)
      throws Exception {
    states
        .withStates()
        .initial(HOLD_INVENTORY)
        .states(EnumSet.allOf(SpringStatemachineBookingSagaState.class))
        .choice(PAYMENT_AUTHORIZATION_DECISION)
        .end(COMPLETED)
        .end(COMPENSATED)
        .end(FAILED);
  }

  @Override
  public void configure(
      StateMachineTransitionConfigurer<
              SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
          transitions)
      throws Exception {
    transitions
        .withExternal()
        .source(HOLD_INVENTORY)
        .target(AUTHORIZE_PAYMENT)
        .event(NEXT)
        .action(actions.holdInventory())
        .and()
        .withExternal()
        .source(AUTHORIZE_PAYMENT)
        .target(PAYMENT_AUTHORIZATION_DECISION)
        .event(NEXT)
        .action(actions.authorizePayment())
        .and()
        .withChoice()
        .source(PAYMENT_AUTHORIZATION_DECISION)
        .first(CONFIRM_BOOKING, actions.paymentAuthorized())
        .last(RELEASE_INVENTORY)
        .and()
        .withExternal()
        .source(CONFIRM_BOOKING)
        .target(APPROVE_PAYMENT)
        .event(NEXT)
        .action(actions.confirmBooking())
        .and()
        .withExternal()
        .source(APPROVE_PAYMENT)
        .target(COMPLETED)
        .event(NEXT)
        .action(actions.approvePayment())
        .and()
        .withExternal()
        .source(CANCEL_PAYMENT)
        .target(RELEASE_INVENTORY)
        .event(NEXT)
        .action(actions.cancelPayment())
        .and()
        .withExternal()
        .source(RELEASE_INVENTORY)
        .target(CANCEL_BOOKING)
        .event(NEXT)
        .action(actions.releaseInventory())
        .and()
        .withExternal()
        .source(CANCEL_BOOKING)
        .target(COMPENSATED)
        .event(NEXT)
        .action(actions.cancelBooking())
        .and()
        .withExternal()
        .source(HOLD_INVENTORY)
        .target(FAILED)
        .event(FAIL)
        .action(failAction())
        .and()
        .withExternal()
        .source(AUTHORIZE_PAYMENT)
        .target(FAILED)
        .event(FAIL)
        .action(failAction())
        .and()
        .withExternal()
        .source(CONFIRM_BOOKING)
        .target(FAILED)
        .event(FAIL)
        .action(failAction())
        .and()
        .withExternal()
        .source(APPROVE_PAYMENT)
        .target(FAILED)
        .event(FAIL)
        .action(failAction());
  }

  private Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
      failAction() {
    return context ->
        SpringStatemachineBookingSagaContext.get(context)
            .markFailed(
                new com.example.hotelbooking.booking.application.saga.BookingSagaFailureReason(
                    "Spring Statemachine prototype failed"));
  }
}
