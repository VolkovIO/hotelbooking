package com.example.hotelbooking.booking.adapter.in.web;

import java.time.Instant;
import java.util.List;

public record BookingApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    List<BookingValidationError> validationErrors) {

  public BookingApiErrorResponse {
    validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
  }

  public static BookingApiErrorResponse of(int status, String error, String message) {
    return new BookingApiErrorResponse(Instant.now(), status, error, message, List.of());
  }

  public static BookingApiErrorResponse validationError(
      List<BookingValidationError> validationErrors) {
    return new BookingApiErrorResponse(
        Instant.now(), 400, "Bad Request", "Request validation failed", validationErrors);
  }
}
