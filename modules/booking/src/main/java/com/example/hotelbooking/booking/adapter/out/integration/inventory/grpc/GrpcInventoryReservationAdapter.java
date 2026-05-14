package com.example.hotelbooking.booking.adapter.out.integration.inventory.grpc;

import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.inventory.grpc.v1.CancelConfirmedReservationRequest;
import com.example.hotelbooking.inventory.grpc.v1.ConfirmHoldRequest;
import com.example.hotelbooking.inventory.grpc.v1.InventoryReservationServiceGrpc;
import com.example.hotelbooking.inventory.grpc.v1.PlaceHoldRequest;
import com.example.hotelbooking.inventory.grpc.v1.ReleaseHoldRequest;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("inventory-grpc-client")
@RequiredArgsConstructor
final class GrpcInventoryReservationAdapter implements InventoryReservationPort {

  private final InventoryReservationServiceGrpc.InventoryReservationServiceBlockingStub
      inventoryReservationStub;

  @Value("${inventory.grpc.client.deadline:PT3S}")
  private Duration deadline;

  @Override
  public UUID placeHold(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    log.debug(
        "Calling inventory gRPC PlaceHold: hotelId={}, roomTypeId={}, checkIn={}, checkOut={}, rooms={}, deadline={}",
        hotelId,
        roomTypeId,
        checkIn,
        checkOut,
        rooms,
        deadline);

    try {
      var response =
          reservationStubWithDeadline()
              .placeHold(
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
      throw BookingInventoryGrpcExceptionMapper.mapFailure(
          "place room hold for hotel %s and room type %s".formatted(hotelId, roomTypeId),
          exception);
    }
  }

  @Override
  public void releaseHold(UUID holdId) {
    log.debug("Calling inventory gRPC ReleaseHold: holdId={}, deadline={}", holdId, deadline);

    try {
      reservationStubWithDeadline()
          .releaseHold(ReleaseHoldRequest.newBuilder().setHoldId(holdId.toString()).build());

      log.debug("Inventory gRPC ReleaseHold completed: holdId={}", holdId);
    } catch (StatusRuntimeException exception) {
      throw BookingInventoryGrpcExceptionMapper.mapFailure(
          "release room hold: " + holdId, exception);
    }
  }

  @Override
  public void confirmHold(UUID holdId) {
    log.debug("Calling inventory gRPC ConfirmHold: holdId={}, deadline={}", holdId, deadline);

    try {
      reservationStubWithDeadline()
          .confirmHold(ConfirmHoldRequest.newBuilder().setHoldId(holdId.toString()).build());

      log.debug("Inventory gRPC ConfirmHold completed: holdId={}", holdId);
    } catch (StatusRuntimeException exception) {
      throw BookingInventoryGrpcExceptionMapper.mapFailure(
          "confirm room hold: " + holdId, exception);
    }
  }

  @Override
  public void cancelConfirmedReservation(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    log.debug(
        "Calling inventory gRPC CancelConfirmedReservation: "
            + "hotelId={}, roomTypeId={}, checkIn={}, checkOut={}, rooms={}, deadline={}",
        hotelId,
        roomTypeId,
        checkIn,
        checkOut,
        rooms,
        deadline);

    try {
      reservationStubWithDeadline()
          .cancelConfirmedReservation(
              CancelConfirmedReservationRequest.newBuilder()
                  .setHotelId(hotelId.toString())
                  .setRoomTypeId(roomTypeId.toString())
                  .setCheckIn(checkIn.toString())
                  .setCheckOut(checkOut.toString())
                  .setRooms(rooms)
                  .build());

      log.debug(
          "Inventory gRPC CancelConfirmedReservation completed: "
              + "hotelId={}, roomTypeId={}, checkIn={}, checkOut={}, rooms={}",
          hotelId,
          roomTypeId,
          checkIn,
          checkOut,
          rooms);
    } catch (StatusRuntimeException exception) {
      throw BookingInventoryGrpcExceptionMapper.mapFailure(
          "cancel confirmed reservation for hotel %s and room type %s"
              .formatted(hotelId, roomTypeId),
          exception);
    }
  }

  private InventoryReservationServiceGrpc.InventoryReservationServiceBlockingStub
      reservationStubWithDeadline() {
    return inventoryReservationStub.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS);
  }
}
