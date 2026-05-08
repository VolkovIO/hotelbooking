package com.example.hotelbooking.notification.application.event;

public class BookingEventRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public BookingEventRejectedException(String message) {
    super(message);
  }

  public BookingEventRejectedException(String message, Throwable cause) {
    super(message, cause);
  }
}
