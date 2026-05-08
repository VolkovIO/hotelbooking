package com.example.hotelbooking.paymentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PaymentOpenApiConfiguration {

  @Bean
  OpenAPI paymentOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Hotel Booking Payment Service API")
                .version("v0.9.0")
                .description(
                    "Payment service API for authorizing, approving and cancelling payments."));
  }
}
