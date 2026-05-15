package com.example.hotelbooking.observability.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Propagates the current SLF4J MDC context to outgoing gRPC calls.
 *
 * <p>The interceptor copies known observability identifiers from MDC into gRPC metadata. The server
 * side can then restore those identifiers into its own MDC and produce logs that belong to the same
 * distributed flow.
 */
public final class MdcGrpcClientInterceptor implements ClientInterceptor {

  @Override
  public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> interceptCall(
      MethodDescriptor<RequestT, ResponseT> method, CallOptions callOptions, Channel next) {
    ClientCall<RequestT, ResponseT> delegate = next.newCall(method, callOptions);

    return new ForwardingClientCall.SimpleForwardingClientCall<>(delegate) {
      @Override
      public void start(Listener<ResponseT> responseListener, Metadata headers) {
        GrpcObservabilityMetadata.copyMdcToHeaders(headers);
        super.start(responseListener, headers);
      }
    };
  }
}
