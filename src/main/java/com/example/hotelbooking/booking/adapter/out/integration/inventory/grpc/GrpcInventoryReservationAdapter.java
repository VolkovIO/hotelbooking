package com.example.hotelbooking.booking.adapter.out.integration.inventory.grpc;

import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.inventory.grpc.v1.CancelConfirmedReservationRequest;
import com.example.hotelbooking.inventory.grpc.v1.ConfirmHoldRequest;
import com.example.hotelbooking.inventory.grpc.v1.InventoryReservationServiceGrpc;
import com.example.hotelbooking.inventory.grpc.v1.PlaceHoldRequest;
import com.example.hotelbooking.inventory.grpc.v1.ReleaseHoldRequest;
import io.grpc.StatusRuntimeException;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("inventory-grpc-client")
@RequiredArgsConstructor
final class GrpcInventoryReservationAdapter implements InventoryReservationPort {

  private final InventoryReservationServiceGrpc.InventoryReservationServiceBlockingStub
      inventoryReservationStub;

  @Override
  public UUID placeHold(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    log.debug(
        "Calling inventory gRPC PlaceHold: hotelId={}, roomTypeId={}, checkIn={}, checkOut={}, rooms={}",
        hotelId,
        roomTypeId,
        checkIn,
        checkOut,
        rooms);

    try {
      var response =
          inventoryReservationStub.placeHold(
              PlaceHoldRequest.newBuilder()
                  .setHotelId(hotelId.toString())
                  .setRoomTypeId(roomTypeId.toString())
                  .setCheckIn(checkIn.toString())
                  .setCheckOut(checkOut.toString())
                  .setRooms(rooms)
                  .build());

      UUID holdId = UUID.fromString(response.getHoldId());
      log.debug("Inventory gRPC PlaceHold completed: holdId={}", holdId);
      return holdId;
    } catch (StatusRuntimeException exception) {
      throw BookingInventoryGrpcExceptionMapper.inventoryCallFailed(
          "place room hold for hotel %s and room type %s".formatted(hotelId, roomTypeId),
          exception);
    }
  }

  @Override
  public void releaseHold(UUID holdId) {
    log.debug("Calling inventory gRPC ReleaseHold: holdId={}", holdId);

    try {
      inventoryReservationStub.releaseHold(
          ReleaseHoldRequest.newBuilder().setHoldId(holdId.toString()).build());
    } catch (StatusRuntimeException exception) {
      throw BookingInventoryGrpcExceptionMapper.inventoryCallFailed(
          "release room hold: " + holdId, exception);
    }
  }

  @Override
  public void confirmHold(UUID holdId) {
    log.debug("Calling inventory gRPC ConfirmHold: holdId={}", holdId);

    try {
      inventoryReservationStub.confirmHold(
          ConfirmHoldRequest.newBuilder().setHoldId(holdId.toString()).build());
    } catch (StatusRuntimeException exception) {
      throw BookingInventoryGrpcExceptionMapper.inventoryCallFailed(
          "confirm room hold: " + holdId, exception);
    }
  }

  @Override
  public void cancelConfirmedReservation(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    log.debug(
        "Call inventory gRPC CancelConfirmedReservation: hotelId={}, roomTypeId={}, checkIn={}, checkOut={}, rooms={}",
        hotelId,
        roomTypeId,
        checkIn,
        checkOut,
        rooms);

    try {
      inventoryReservationStub.cancelConfirmedReservation(
          CancelConfirmedReservationRequest.newBuilder()
              .setHotelId(hotelId.toString())
              .setRoomTypeId(roomTypeId.toString())
              .setCheckIn(checkIn.toString())
              .setCheckOut(checkOut.toString())
              .setRooms(rooms)
              .build());
    } catch (StatusRuntimeException exception) {
      throw BookingInventoryGrpcExceptionMapper.inventoryCallFailed(
          "cancel confirmed reservation for hotel %s and room type %s"
              .formatted(hotelId, roomTypeId),
          exception);
    }
  }
}
