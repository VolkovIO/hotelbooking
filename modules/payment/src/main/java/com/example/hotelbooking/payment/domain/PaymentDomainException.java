package com.example.hotelbooking.payment.domain;

public class PaymentDomainException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public PaymentDomainException(String message) {
    super(message);
  }
}
