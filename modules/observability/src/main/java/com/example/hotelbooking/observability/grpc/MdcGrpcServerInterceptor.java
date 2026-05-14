package com.example.hotelbooking.observability.grpc;

import com.example.hotelbooking.observability.logging.ObservabilityContext;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * Restores observability identifiers from incoming gRPC metadata into SLF4J MDC.
 *
 * <p>gRPC invokes listener callbacks on its own worker threads. MDC is thread-local, so the
 * interceptor opens the context separately for every listener callback where application code may
 * run.
 */
public final class MdcGrpcServerInterceptor implements ServerInterceptor {

  @Override
  public <RequestT, ResponseT> ServerCall.Listener<RequestT> interceptCall(
      ServerCall<RequestT, ResponseT> call,
      Metadata headers,
      ServerCallHandler<RequestT, ResponseT> next) {
    ServerCall.Listener<RequestT> delegate = next.startCall(call, headers);

    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
      @Override
      public void onMessage(RequestT message) {
        try (ObservabilityContext ignored =
            GrpcObservabilityMetadata.openContextFromHeaders(headers)) {
          super.onMessage(message);
        }
      }

      @Override
      public void onHalfClose() {
        try (ObservabilityContext ignored =
            GrpcObservabilityMetadata.openContextFromHeaders(headers)) {
          super.onHalfClose();
        }
      }

      @Override
      public void onCancel() {
        try (ObservabilityContext ignored =
            GrpcObservabilityMetadata.openContextFromHeaders(headers)) {
          super.onCancel();
        }
      }

      @Override
      public void onComplete() {
        try (ObservabilityContext ignored =
            GrpcObservabilityMetadata.openContextFromHeaders(headers)) {
          super.onComplete();
        }
      }

      @Override
      public void onReady() {
        try (ObservabilityContext ignored =
            GrpcObservabilityMetadata.openContextFromHeaders(headers)) {
          super.onReady();
        }
      }
    };
  }
}
