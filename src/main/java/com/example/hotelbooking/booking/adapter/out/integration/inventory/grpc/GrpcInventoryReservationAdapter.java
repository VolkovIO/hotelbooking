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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("inventory-grpc-client")
@RequiredArgsConstructor
final class GrpcInventoryReservationAdapter implements InventoryReservationPort {

  private final InventoryReservationServiceGrpc.InventoryReservationServiceBlockingStub
      inventoryReservationStub;

  @Override
  public UUID placeHold(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
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

      return UUID.fromString(response.getHoldId());
    } catch (StatusRuntimeException exception) {
      throw BookingInventoryGrpcExceptionMapper.inventoryCallFailed(
          "place room hold for hotel %s and room type %s".formatted(hotelId, roomTypeId),
          exception);
    }
  }

  @Override
  public void releaseHold(UUID holdId) {
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
