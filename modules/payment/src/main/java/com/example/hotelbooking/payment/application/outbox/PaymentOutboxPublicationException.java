package com.example.hotelbooking.payment.application.outbox;

public class PaymentOutboxPublicationException extends Exception {

  private static final long serialVersionUID = 1L;

  public PaymentOutboxPublicationException(String message, Throwable cause) {
    super(message, cause);
  }
}
