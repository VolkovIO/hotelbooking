package com.example.hotelbooking.payment.application.outbox;

public enum PaymentOutboxStatus {
  NEW,
  PROCESSING,
  PUBLISHED,
  FAILED
}
