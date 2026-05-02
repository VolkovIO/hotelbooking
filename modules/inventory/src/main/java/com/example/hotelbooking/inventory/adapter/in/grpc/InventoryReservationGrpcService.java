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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReservationGrpcService
    extends InventoryReservationServiceGrpc.InventoryReservationServiceImplBase {

  private final InventoryReservationUseCase inventoryReservationUseCase;

  @Override
  public void placeHold(
      PlaceHoldRequest request, StreamObserver<PlaceHoldResponse> responseObserver) {
    log.debug(
        "Received inventory gRPC PlaceHold: hotelId={}, roomTypeId={}, checkIn={}, checkOut={}, rooms={}",
        request.getHotelId(),
        request.getRoomTypeId(),
        request.getCheckIn(),
        request.getCheckOut(),
        request.getRooms());

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

          log.debug("Inventory gRPC PlaceHold completed: holdId={}", holdId);

          return PlaceHoldResponse.newBuilder().setHoldId(holdId.toString()).build();
        });
  }

  @Override
  public void confirmHold(
      ConfirmHoldRequest request, StreamObserver<ConfirmHoldResponse> responseObserver) {
    log.debug("Received inventory gRPC ConfirmHold: holdId={}", request.getHoldId());

    InventoryGrpcExceptionMapper.handle(
        responseObserver,
        () -> {
          inventoryReservationUseCase.confirmHold(
              InventoryGrpcMapper.toUuid(request.getHoldId(), "holdId"));

          log.debug("Inventory gRPC ConfirmHold completed: holdId={}", request.getHoldId());

          return ConfirmHoldResponse.newBuilder().build();
        });
  }

  @Override
  public void releaseHold(
      ReleaseHoldRequest request, StreamObserver<ReleaseHoldResponse> responseObserver) {
    log.debug("Received inventory gRPC ReleaseHold: holdId={}", request.getHoldId());

    InventoryGrpcExceptionMapper.handle(
        responseObserver,
        () -> {
          inventoryReservationUseCase.releaseHold(
              InventoryGrpcMapper.toUuid(request.getHoldId(), "holdId"));

          log.debug("Inventory gRPC ReleaseHold completed: holdId={}", request.getHoldId());

          return ReleaseHoldResponse.newBuilder().build();
        });
  }

  @Override
  public void cancelConfirmedReservation(
      CancelConfirmedReservationRequest request,
      StreamObserver<CancelConfirmedReservationResponse> responseObserver) {
    log.debug(
        "Received inventory gRPC CancelConfirmedReservation: "
            + "hotelId={}, roomTypeId={}, checkIn={}, checkOut={}, rooms={}",
        request.getHotelId(),
        request.getRoomTypeId(),
        request.getCheckIn(),
        request.getCheckOut(),
        request.getRooms());

    InventoryGrpcExceptionMapper.handle(
        responseObserver,
        () -> {
          inventoryReservationUseCase.cancelConfirmedReservation(
              InventoryGrpcMapper.toUuid(request.getHotelId(), "hotelId"),
              InventoryGrpcMapper.toUuid(request.getRoomTypeId(), "roomTypeId"),
              InventoryGrpcMapper.toLocalDate(request.getCheckIn(), "checkIn"),
              InventoryGrpcMapper.toLocalDate(request.getCheckOut(), "checkOut"),
              request.getRooms());

          log.debug(
              "Inventory gRPC CancelConfirmedReservation completed: "
                  + "hotelId={}, roomTypeId={}, checkIn={}, checkOut={}, rooms={}",
              request.getHotelId(),
              request.getRoomTypeId(),
              request.getCheckIn(),
              request.getCheckOut(),
              request.getRooms());

          return CancelConfirmedReservationResponse.newBuilder().build();
        });
  }
}
