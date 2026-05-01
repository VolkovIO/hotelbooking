package com.example.hotelbooking.booking.adapter.out.integration.inventory.grpc;

import com.example.hotelbooking.booking.application.port.out.InventoryLookupPort;
import com.example.hotelbooking.booking.application.port.out.RoomTypeReference;
import com.example.hotelbooking.inventory.grpc.v1.FindRoomTypeReferenceRequest;
import com.example.hotelbooking.inventory.grpc.v1.InventoryQueryServiceGrpc;
import io.grpc.StatusRuntimeException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("inventory-grpc-client")
@RequiredArgsConstructor
final class GrpcInventoryLookupAdapter implements InventoryLookupPort {

  private final InventoryQueryServiceGrpc.InventoryQueryServiceBlockingStub inventoryQueryStub;

  @Override
  public Optional<RoomTypeReference> findRoomTypeReference(UUID hotelId, UUID roomTypeId) {
    try {
      var response =
          inventoryQueryStub.findRoomTypeReference(
              FindRoomTypeReferenceRequest.newBuilder()
                  .setHotelId(hotelId.toString())
                  .setRoomTypeId(roomTypeId.toString())
                  .build());

      if (!response.getFound()) {
        return Optional.empty();
      }

      var roomType = response.getRoomType();

      return Optional.of(
          new RoomTypeReference(
              UUID.fromString(roomType.getHotelId()),
              UUID.fromString(roomType.getRoomTypeId()),
              roomType.getGuestCapacity()));
    } catch (StatusRuntimeException exception) {
      throw BookingInventoryGrpcExceptionMapper.inventoryCallFailed(
          "find room type reference for hotel %s and room type %s".formatted(hotelId, roomTypeId),
          exception);
    }
  }
}
