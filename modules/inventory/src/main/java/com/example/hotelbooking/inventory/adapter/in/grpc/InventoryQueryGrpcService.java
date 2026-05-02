package com.example.hotelbooking.inventory.adapter.in.grpc;

import com.example.hotelbooking.inventory.application.port.in.InventoryQueryUseCase;
import com.example.hotelbooking.inventory.grpc.v1.FindRoomTypeReferenceRequest;
import com.example.hotelbooking.inventory.grpc.v1.FindRoomTypeReferenceResponse;
import com.example.hotelbooking.inventory.grpc.v1.InventoryQueryServiceGrpc;
import com.example.hotelbooking.inventory.grpc.v1.RoomTypeReference;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryQueryGrpcService
    extends InventoryQueryServiceGrpc.InventoryQueryServiceImplBase {

  private final InventoryQueryUseCase inventoryQueryUseCase;

  @Override
  public void findRoomTypeReference(
      FindRoomTypeReferenceRequest request,
      StreamObserver<FindRoomTypeReferenceResponse> responseObserver) {
    log.debug(
        "Received inventory gRPC FindRoomTypeReference: hotelId={}, roomTypeId={}",
        request.getHotelId(),
        request.getRoomTypeId());

    InventoryGrpcExceptionMapper.handle(
        responseObserver,
        () ->
            inventoryQueryUseCase
                .findRoomTypeReference(
                    InventoryGrpcMapper.toUuid(request.getHotelId(), "hotelId"),
                    InventoryGrpcMapper.toUuid(request.getRoomTypeId(), "roomTypeId"))
                .map(
                    result -> {
                      log.debug(
                          "Inventory gRPC FindRoomTypeReference found: hotelId={}, roomTypeId={}, guestCapacity={}",
                          result.hotelId(),
                          result.roomTypeId(),
                          result.guestCapacity());

                      return FindRoomTypeReferenceResponse.newBuilder()
                          .setFound(true)
                          .setRoomType(
                              RoomTypeReference.newBuilder()
                                  .setHotelId(result.hotelId().toString())
                                  .setRoomTypeId(result.roomTypeId().toString())
                                  .setGuestCapacity(result.guestCapacity())
                                  .build())
                          .build();
                    })
                .orElseGet(
                    () -> {
                      log.debug(
                          "Inventory gRPC FindRoomTypeReference not found: hotelId={}, roomTypeId={}",
                          request.getHotelId(),
                          request.getRoomTypeId());

                      return FindRoomTypeReferenceResponse.newBuilder().setFound(false).build();
                    }));
  }
}
