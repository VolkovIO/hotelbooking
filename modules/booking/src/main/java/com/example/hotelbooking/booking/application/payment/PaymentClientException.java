package com.example.hotelbooking.booking.application.payment;

import java.io.Serial;

public class PaymentClientException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public PaymentClientException(String message, Throwable cause) {
    super(message, cause);
  }

  public PaymentClientException(String message) {
    super(message);
  }
}
