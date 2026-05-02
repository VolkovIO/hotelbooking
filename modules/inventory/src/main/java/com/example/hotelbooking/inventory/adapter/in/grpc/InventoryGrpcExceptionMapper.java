package com.example.hotelbooking.inventory.adapter.in.grpc;

import com.example.hotelbooking.inventory.application.exception.InventoryApplicationException;
import com.example.hotelbooking.inventory.domain.InventoryDomainException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

final class InventoryGrpcExceptionMapper {

  private InventoryGrpcExceptionMapper() {}

  static <T> void handle(StreamObserver<T> responseObserver, GrpcCall<T> call) {
    try {
      T response = call.execute();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (InventoryApplicationException exception) {
      responseObserver.onError(toStatusRuntimeException(exception));
    } catch (InventoryDomainException exception) {
      responseObserver.onError(toStatusRuntimeException(exception));
    } catch (StatusRuntimeException exception) {
      responseObserver.onError(exception);
    }
  }

  private static StatusRuntimeException toStatusRuntimeException(
      InventoryApplicationException exception) {
    return Status.FAILED_PRECONDITION
        .withDescription(exception.getMessage())
        .withCause(exception)
        .asRuntimeException();
  }

  private static StatusRuntimeException toStatusRuntimeException(
      InventoryDomainException exception) {
    return Status.INVALID_ARGUMENT
        .withDescription(exception.getMessage())
        .withCause(exception)
        .asRuntimeException();
  }

  @FunctionalInterface
  interface GrpcCall<T> {
    T execute();
  }
}
