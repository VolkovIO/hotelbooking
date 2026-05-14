package com.example.hotelbooking.inventory.adapter.in.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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
      ObjectProvider<ServerInterceptor> grpcServerInterceptors,
      @Value("${inventory.grpc.server.port}") int port,
      @Value("${inventory.grpc.server.tls.enabled:false}") boolean tlsEnabled,
      @Value("${inventory.grpc.server.tls.certificate-chain:}") String certificateChainPath,
      @Value("${inventory.grpc.server.tls.private-key:}") String privateKeyPath,
      @Value("${inventory.grpc.server.tls.trust-cert-collection:}") String trustCertCollectionPath,
      @Value("${inventory.grpc.server.mtls.allowed-client-common-name:booking-service}")
          String allowedClientCommonName)
      throws SSLException {
    log.info(
        "Starting inventory gRPC server: port={}, tlsEnabled={}, allowedClientCommonName={}",
        port,
        tlsEnabled,
        allowedClientCommonName);

    List<ServerInterceptor> interceptors = grpcServerInterceptors.orderedStream().toList();

    if (!tlsEnabled) {
      log.info("Configuring inventory gRPC server interceptors: count={}", interceptors.size());

      return ServerBuilder.forPort(port)
          .addService(intercept(inventoryQueryGrpcService, interceptors))
          .addService(intercept(inventoryReservationGrpcService, interceptors))
          .build();
    }

    List<ServerInterceptor> tlsInterceptors = new ArrayList<>(interceptors);
    tlsInterceptors.add(new InventoryGrpcServiceIdentityInterceptor(allowedClientCommonName));

    log.info("Configuring inventory gRPC server interceptors: count={}", tlsInterceptors.size());

    SslContext sslContext =
        GrpcSslContexts.forServer(
                requiredFile(certificateChainPath, "inventory gRPC server certificate"),
                requiredFile(privateKeyPath, "inventory gRPC server private key"))
            .trustManager(requiredFile(trustCertCollectionPath, "inventory gRPC trust certificate"))
            .clientAuth(ClientAuth.REQUIRE)
            .build();

    return NettyServerBuilder.forPort(port)
        .sslContext(sslContext)
        .addService(intercept(inventoryQueryGrpcService, tlsInterceptors))
        .addService(intercept(inventoryReservationGrpcService, tlsInterceptors))
        .build();
  }

  private ServerServiceDefinition intercept(
      BindableService service, List<ServerInterceptor> interceptors) {
    if (interceptors.isEmpty()) {
      return service.bindService();
    }

    return ServerInterceptors.intercept(service, interceptors);
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
