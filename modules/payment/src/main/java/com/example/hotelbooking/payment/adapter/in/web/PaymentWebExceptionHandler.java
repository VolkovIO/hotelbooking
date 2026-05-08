package com.example.hotelbooking.payment.adapter.in.web;

import com.example.hotelbooking.payment.application.exception.PaymentNotFoundException;
import com.example.hotelbooking.payment.domain.PaymentDomainException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class PaymentWebExceptionHandler {

  private static final String PAYMENT_NOT_FOUND = "PAYMENT_NOT_FOUND";
  private static final String PAYMENT_INVALID_STATE = "PAYMENT_INVALID_STATE";
  private static final String PAYMENT_BAD_REQUEST = "PAYMENT_BAD_REQUEST";
  private static final String PAYMENT_VALIDATION_FAILED = "PAYMENT_VALIDATION_FAILED";

  @ExceptionHandler(PaymentNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  PaymentApiErrorResponse handlePaymentNotFound(PaymentNotFoundException exception) {
    return PaymentApiErrorResponse.of(PAYMENT_NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(PaymentDomainException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  PaymentApiErrorResponse handlePaymentDomainException(PaymentDomainException exception) {
    return PaymentApiErrorResponse.of(PAYMENT_INVALID_STATE, exception.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  PaymentApiErrorResponse handleIllegalArgumentException(IllegalArgumentException exception) {
    return PaymentApiErrorResponse.of(PAYMENT_BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  PaymentApiErrorResponse handleValidationException(MethodArgumentNotValidException exception) {
    String message = exception.getBindingResult().getAllErrors().getFirst().getDefaultMessage();

    return PaymentApiErrorResponse.of(PAYMENT_VALIDATION_FAILED, message);
  }
}
