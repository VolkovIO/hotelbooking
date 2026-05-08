package com.example.hotelbooking.payment.adapter.in.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PaymentHealthController {

  @GetMapping("/api/v1/payments/health")
  Map<String, String> health() {
    return Map.of("status", "OK", "service", "payment-service");
  }
}
