package com.example.hotelbooking.booking.adapter.out.integration.inventory.grpc;

import com.example.hotelbooking.booking.application.exception.RoomHoldFailedException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

final class BookingInventoryGrpcExceptionMapper {

  private BookingInventoryGrpcExceptionMapper() {}

  static RoomHoldFailedException inventoryCallFailed(
      String operation, StatusRuntimeException exception) {
    Status.Code code = exception.getStatus().getCode();

    if (code == Status.Code.NOT_FOUND
        || code == Status.Code.INVALID_ARGUMENT
        || code == Status.Code.FAILED_PRECONDITION
        || code == Status.Code.RESOURCE_EXHAUSTED) {
      return new RoomHoldFailedException("Failed to " + operation, exception);
    }

    if (code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED) {
      return new RoomHoldFailedException(
          "Inventory service is unavailable while trying to " + operation, exception);
    }

    return new RoomHoldFailedException(
        "Unexpected inventory service error while trying to " + operation, exception);
  }
}
