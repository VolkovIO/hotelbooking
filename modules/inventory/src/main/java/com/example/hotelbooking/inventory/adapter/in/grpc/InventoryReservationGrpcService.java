package com.example.hotelbooking.inventory.adapter.in.grpc;

import com.example.hotelbooking.inventory.application.port.in.InventoryReservationUseCase;
import com.example.hotelbooking.inventory.grpc.v1.CancelConfirmedReservationRequest;
import com.example.hotelbooking.inventory.grpc.v1.CancelConfirmedReservationResponse;
import com.example.hotelbooking.inventory.grpc.v1.ConfirmHoldRequest;
import com.example.hotelbooking.inventory.grpc.v1.ConfirmHoldResponse;
import com.example.hotelbooking.inventory.grpc.v1.InventoryReservationServiceGrpc;
import com.example.hotelbooking.inventory.grpc.v1.PlaceHoldRequest;
import com.example.hotelbooking.inventory.grpc.v1.PlaceHoldResponse;
import com.example.hotelbooking.inventory.grpc.v1.ReleaseHoldRequest;
import com.example.hotelbooking.inventory.grpc.v1.ReleaseHoldResponse;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryReservationGrpcService
    extends InventoryReservationServiceGrpc.InventoryReservationServiceImplBase {

  private final InventoryReservationUseCase inventoryReservationUseCase;

  @Override
  public void placeHold(
      PlaceHoldRequest request, StreamObserver<PlaceHoldResponse> responseObserver) {
    InventoryGrpcExceptionMapper.handle(
        responseObserver,
        () -> {
          UUID holdId =
              inventoryReservationUseCase.placeHold(
                  InventoryGrpcMapper.toUuid(request.getHotelId(), "hotelId"),
                  InventoryGrpcMapper.toUuid(request.getRoomTypeId(), "roomTypeId"),
                  InventoryGrpcMapper.toLocalDate(request.getCheckIn(), "checkIn"),
                  InventoryGrpcMapper.toLocalDate(request.getCheckOut(), "checkOut"),
                  request.getRooms());

          return PlaceHoldResponse.newBuilder().setHoldId(holdId.toString()).build();
        });
  }

  @Override
  public void confirmHold(
      ConfirmHoldRequest request, StreamObserver<ConfirmHoldResponse> responseObserver) {
    InventoryGrpcExceptionMapper.handle(
        responseObserver,
        () -> {
          inventoryReservationUseCase.confirmHold(
              InventoryGrpcMapper.toUuid(request.getHoldId(), "holdId"));
          return ConfirmHoldResponse.newBuilder().build();
        });
  }

  @Override
  public void releaseHold(
      ReleaseHoldRequest request, StreamObserver<ReleaseHoldResponse> responseObserver) {
    InventoryGrpcExceptionMapper.handle(
        responseObserver,
        () -> {
          inventoryReservationUseCase.releaseHold(
              InventoryGrpcMapper.toUuid(request.getHoldId(), "holdId"));
          return ReleaseHoldResponse.newBuilder().build();
        });
  }

  @Override
  public void cancelConfirmedReservation(
      CancelConfirmedReservationRequest request,
      StreamObserver<CancelConfirmedReservationResponse> responseObserver) {
    InventoryGrpcExceptionMapper.handle(
        responseObserver,
        () -> {
          inventoryReservationUseCase.cancelConfirmedReservation(
              InventoryGrpcMapper.toUuid(request.getHotelId(), "hotelId"),
              InventoryGrpcMapper.toUuid(request.getRoomTypeId(), "roomTypeId"),
              InventoryGrpcMapper.toLocalDate(request.getCheckIn(), "checkIn"),
              InventoryGrpcMapper.toLocalDate(request.getCheckOut(), "checkOut"),
              request.getRooms());

          return CancelConfirmedReservationResponse.newBuilder().build();
        });
  }
}
