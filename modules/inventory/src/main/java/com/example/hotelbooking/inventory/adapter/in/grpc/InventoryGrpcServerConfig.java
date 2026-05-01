package com.example.hotelbooking.inventory.adapter.in.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("inventory-grpc-server")
public class InventoryGrpcServerConfig {

  @Bean(initMethod = "start", destroyMethod = "shutdown")
  Server inventoryGrpcServer(
      InventoryQueryGrpcService inventoryQueryGrpcService,
      InventoryReservationGrpcService inventoryReservationGrpcService,
      @Value("${inventory.grpc.server.port}") int port) {

    log.info("Starting inventory gRPC server on port {}", port);
    return ServerBuilder.forPort(port)
        .addService(inventoryQueryGrpcService)
        .addService(inventoryReservationGrpcService)
        .build();
  }
}
