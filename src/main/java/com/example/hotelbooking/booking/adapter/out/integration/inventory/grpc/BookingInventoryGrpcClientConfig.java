package com.example.hotelbooking.booking.adapter.out.integration.inventory.grpc;

import com.example.hotelbooking.inventory.grpc.v1.InventoryQueryServiceGrpc;
import com.example.hotelbooking.inventory.grpc.v1.InventoryReservationServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("inventory-grpc-client")
public class BookingInventoryGrpcClientConfig {

  @Bean(destroyMethod = "shutdown")
  ManagedChannel inventoryManagedChannel(
      @Value("${inventory.grpc.client.host}") String host,
      @Value("${inventory.grpc.client.port}") int port) {
    return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
  }

  @Bean
  InventoryQueryServiceGrpc.InventoryQueryServiceBlockingStub inventoryQueryServiceBlockingStub(
      ManagedChannel inventoryManagedChannel) {
    return InventoryQueryServiceGrpc.newBlockingStub(inventoryManagedChannel);
  }

  @Bean
  InventoryReservationServiceGrpc.InventoryReservationServiceBlockingStub
      inventoryReservationServiceBlockingStub(ManagedChannel inventoryManagedChannel) {
    return InventoryReservationServiceGrpc.newBlockingStub(inventoryManagedChannel);
  }
}
