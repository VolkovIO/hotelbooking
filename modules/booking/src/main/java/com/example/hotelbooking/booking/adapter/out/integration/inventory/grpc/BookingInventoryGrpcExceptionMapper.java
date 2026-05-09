package com.example.hotelbooking.booking.adapter.out.integration.inventory.grpc;

import com.example.hotelbooking.booking.application.exception.BookingInventoryException;
import com.example.hotelbooking.booking.application.exception.InventoryReservationException;
import com.example.hotelbooking.booking.application.exception.RoomHoldFailedException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class BookingInventoryGrpcExceptionMapper {

  private BookingInventoryGrpcExceptionMapper() {}

  static BookingInventoryException mapFailure(String operation, StatusRuntimeException exception) {
    logFailure(operation, exception);

    if (isBusinessFailure(exception)) {
      return new RoomHoldFailedException("Failed to " + operation, exception);
    }

    Status.Code code = exception.getStatus().getCode();

    if (code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED) {
      return new InventoryReservationException(
          "Inventory service is unavailable while trying to " + operation, exception);
    }

    return new InventoryReservationException(
        "Unexpected inventory service error while trying to " + operation, exception);
  }

  static InventoryReservationException technicalFailure(
      String operation, StatusRuntimeException exception) {
    logFailure(operation, exception);

    Status.Code code = exception.getStatus().getCode();

    if (code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED) {
      return new InventoryReservationException(
          "Inventory service is unavailable while trying to " + operation, exception);
    }

    return new InventoryReservationException(
        "Unexpected inventory service error while trying to " + operation, exception);
  }

  static boolean isBusinessFailure(StatusRuntimeException exception) {
    Status.Code code = exception.getStatus().getCode();

    return code == Status.Code.NOT_FOUND
        || code == Status.Code.INVALID_ARGUMENT
        || code == Status.Code.FAILED_PRECONDITION
        || code == Status.Code.RESOURCE_EXHAUSTED;
  }

  private static void logFailure(String operation, StatusRuntimeException exception) {
    log.warn(
        "Inventory gRPC call failed: operation={}, status={}, description={}",
        operation,
        exception.getStatus().getCode(),
        exception.getStatus().getDescription());
  }
}
