package com.example.hotelbooking.booking.adapter.out.integration.inventory.grpc;

import com.example.hotelbooking.inventory.grpc.v1.InventoryQueryServiceGrpc;
import com.example.hotelbooking.inventory.grpc.v1.InventoryReservationServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import java.io.File;
import javax.net.ssl.SSLException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("inventory-grpc-client")
public class BookingInventoryGrpcClientConfig {

  @Bean(destroyMethod = "shutdown")
  ManagedChannel inventoryManagedChannel(
      @Value("${inventory.grpc.client.host}") String host,
      @Value("${inventory.grpc.client.port}") int port,
      @Value("${inventory.grpc.client.tls.enabled:false}") boolean tlsEnabled,
      @Value("${inventory.grpc.client.tls.certificate-chain:}") String certificateChainPath,
      @Value("${inventory.grpc.client.tls.private-key:}") String privateKeyPath,
      @Value("${inventory.grpc.client.tls.trust-cert-collection:}") String trustCertCollectionPath)
      throws SSLException {
    log.info(
        "Creating inventory gRPC client channel: host={}, port={}, tlsEnabled={}",
        host,
        port,
        tlsEnabled);

    if (!tlsEnabled) {
      return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    }

    SslContext sslContext =
        GrpcSslContexts.forClient()
            .trustManager(requiredFile(trustCertCollectionPath, "inventory gRPC trust certificate"))
            .keyManager(
                requiredFile(certificateChainPath, "booking gRPC client certificate"),
                requiredFile(privateKeyPath, "booking gRPC client private key"))
            .build();

    return NettyChannelBuilder.forAddress(host, port).sslContext(sslContext).build();
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

  private File requiredFile(String path, String description) {
    if (path == null || path.isBlank()) {
      throw new IllegalStateException(description + " path must be configured");
    }

    File file = new File(path);

    if (!file.isFile()) {
      throw new IllegalStateException(description + " does not exist: " + path);
    }

    return file;
  }
}
