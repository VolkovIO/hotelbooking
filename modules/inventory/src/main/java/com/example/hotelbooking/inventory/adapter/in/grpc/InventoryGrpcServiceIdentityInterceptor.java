package com.example.hotelbooking.inventory.adapter.in.grpc;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.security.Principal;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
class InventoryGrpcServiceIdentityInterceptor implements ServerInterceptor {

  private static final String COMMON_NAME_ATTRIBUTE = "CN";

  private final String allowedClientCommonName;

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    SSLSession sslSession = call.getAttributes().get(Grpc.TRANSPORT_ATTR_SSL_SESSION);

    if (sslSession == null) {
      closeCall(call, Status.UNAUTHENTICATED.withDescription("mTLS session is missing"));
      return new ClosedServerCallListener<>();
    }

    String clientCommonName = extractClientCommonName(sslSession);

    if (!allowedClientCommonName.equals(clientCommonName)) {
      log.warn(
          "Rejected inventory gRPC client: expectedCommonName={}, actualCommonName={}",
          allowedClientCommonName,
          clientCommonName);

      closeCall(
          call, Status.PERMISSION_DENIED.withDescription("Client certificate is not allowed"));
      return new ClosedServerCallListener<>();
    }

    return next.startCall(call, headers);
  }

  private String extractClientCommonName(SSLSession sslSession) {
    try {
      Principal peerPrincipal = sslSession.getPeerPrincipal();
      return commonNameFromDistinguishedName(peerPrincipal.getName());
    } catch (SSLPeerUnverifiedException exception) {
      throw new IllegalStateException("Client certificate is not verified", exception);
    }
  }

  private String commonNameFromDistinguishedName(String distinguishedName) {
    try {
      LdapName ldapName = new LdapName(distinguishedName);

      for (Rdn rdn : ldapName.getRdns()) {
        if (COMMON_NAME_ATTRIBUTE.equalsIgnoreCase(rdn.getType())) {
          return String.valueOf(rdn.getValue());
        }
      }

      throw new IllegalStateException("Client certificate CN is missing: " + distinguishedName);
    } catch (InvalidNameException exception) {
      throw new IllegalStateException(
          "Client certificate subject is not a valid distinguished name", exception);
    }
  }

  private <ReqT, RespT> void closeCall(ServerCall<ReqT, RespT> call, Status status) {
    call.close(status, new Metadata());
  }

  private static final class ClosedServerCallListener<ReqT>
      extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

    private ClosedServerCallListener() {
      super(new ServerCall.Listener<>() {});
    }
  }
}
