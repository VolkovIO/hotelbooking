package com.example.hotelbooking.inventory.adapter.in.web;

import java.time.Instant;
import java.util.List;

public record InventoryApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    List<InventoryValidationError> validationErrors) {

  public InventoryApiErrorResponse {
    validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
  }

  public static InventoryApiErrorResponse of(int status, String error, String message) {
    return new InventoryApiErrorResponse(Instant.now(), status, error, message, List.of());
  }

  public static InventoryApiErrorResponse validationError(
      List<InventoryValidationError> validationErrors) {
    return new InventoryApiErrorResponse(
        Instant.now(), 400, "Bad Request", "Request validation failed", validationErrors);
  }
}
