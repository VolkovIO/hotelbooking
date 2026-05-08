package com.example.hotelbooking.payment.adapter.in.web;

import java.time.Instant;

record PaymentApiErrorResponse(String code, String message, Instant timestamp) {

  static PaymentApiErrorResponse of(String code, String message) {
    return new PaymentApiErrorResponse(code, message, Instant.now());
  }
}
