package com.example.hotelbooking.booking.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Standard API error response")
public record ApiErrorResponse(
    @Schema(description = "Error code", example = "BOOKING_DOMAIN_ERROR") String code,
    @Schema(
            description = "Human-readable error message",
            example = "checkOut must be after checkIn")
        String message,
    @Schema(description = "Request path", example = "/api/v1/bookings") String path,
    @Schema(description = "Response timestamp in UTC", example = "2026-04-14T10:15:30Z")
        Instant timestamp,
    @Schema(description = "Validation error details if present")
        List<ApiValidationError> validationErrors) {

  public ApiErrorResponse {
    validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
  }

  public static ApiErrorResponse of(String code, String message, String path) {
    return new ApiErrorResponse(code, message, path, Instant.now(), List.of());
  }

  public static ApiErrorResponse ofValidationErrors(
      String code, String message, String path, List<ApiValidationError> validationErrors) {
    return new ApiErrorResponse(code, message, path, Instant.now(), validationErrors);
  }
}
