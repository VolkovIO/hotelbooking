package com.example.hotelbooking.booking.application.saga.springstatemachine;

enum SpringStatemachineBookingSagaState {
  HOLD_INVENTORY,
  AUTHORIZE_PAYMENT,
  PAYMENT_AUTHORIZATION_DECISION,
  CONFIRM_BOOKING,
  APPROVE_PAYMENT,
  CANCEL_PAYMENT,
  RELEASE_INVENTORY,
  CANCEL_BOOKING,
  COMPLETED,
  COMPENSATED,
  FAILED
}
