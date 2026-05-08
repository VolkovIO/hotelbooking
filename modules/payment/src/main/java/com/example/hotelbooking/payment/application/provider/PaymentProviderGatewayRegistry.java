package com.example.hotelbooking.payment.application.provider;

import com.example.hotelbooking.payment.domain.PaymentProvider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PaymentProviderGatewayRegistry {

  private final Map<PaymentProvider, PaymentProviderGateway> gateways;

  public PaymentProviderGatewayRegistry(List<PaymentProviderGateway> gateways) {
    this.gateways = new EnumMap<>(PaymentProvider.class);

    for (PaymentProviderGateway gateway : gateways) {
      this.gateways.put(gateway.provider(), gateway);
    }
  }

  public PaymentProviderGateway getGateway(PaymentProvider provider) {
    PaymentProviderGateway gateway = gateways.get(provider);

    if (gateway == null) {
      throw new PaymentProviderGatewayNotFoundException(provider);
    }

    return gateway;
  }
}
