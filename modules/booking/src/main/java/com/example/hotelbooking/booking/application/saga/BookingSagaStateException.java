package com.example.hotelbooking.booking.application.saga;

public class BookingSagaStateException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public BookingSagaStateException(String message) {
    super(message);
  }
}
