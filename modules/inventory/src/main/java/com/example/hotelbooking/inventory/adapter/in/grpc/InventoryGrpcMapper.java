package com.example.hotelbooking.inventory.adapter.in.grpc;

import io.grpc.Status;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

final class InventoryGrpcMapper {

  private InventoryGrpcMapper() {}

  static UUID toUuid(String value, String fieldName) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw Status.INVALID_ARGUMENT
          .withDescription("Invalid UUID value for " + fieldName + ": " + value)
          .withCause(exception)
          .asRuntimeException();
    }
  }

  static LocalDate toLocalDate(String value, String fieldName) {
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException exception) {
      throw Status.INVALID_ARGUMENT
          .withDescription("Invalid date value for " + fieldName + ": " + value)
          .withCause(exception)
          .asRuntimeException();
    }
  }
}
