package com.example.hotelbooking.payment.adapter.out.provider.fake;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.payment.provider.fake")
class FakePaymentProviderProperties {

  private boolean enabled = true;
  private boolean alwaysDecline;
  private BigDecimal declineAmountGreaterThan;
}
