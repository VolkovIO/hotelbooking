package com.example.hotelbooking.booking.api;

import com.example.hotelbooking.booking.domain.BookingDomainException;
import com.example.hotelbooking.booking.domain.BookingNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BookingNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse handleBookingNotFoundException(
      BookingNotFoundException exception, HttpServletRequest request) {
    return ApiErrorResponse.of(
        "BOOKING_NOT_FOUND", exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(BookingDomainException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleBookingDomainException(
      BookingDomainException exception, HttpServletRequest request) {
    return ApiErrorResponse.of(
        "BOOKING_DOMAIN_ERROR", exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleIllegalArgumentException(
      IllegalArgumentException exception, HttpServletRequest request) {
    return ApiErrorResponse.of("INVALID_REQUEST", exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleMethodArgumentNotValidException(
      MethodArgumentNotValidException exception, HttpServletRequest request) {

    List<ApiValidationError> validationErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new ApiValidationError(error.getField(), error.getDefaultMessage()))
            .toList();

    return ApiErrorResponse.ofValidationErrors(
        "VALIDATION_ERROR", "Request validation failed", request.getRequestURI(), validationErrors);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleHttpMessageNotReadableException(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return ApiErrorResponse.of(
        "MALFORMED_REQUEST_BODY",
        "Request body is malformed or contains invalid field values",
        request.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
    return ApiErrorResponse.of(
        "INVALID_PARAMETER", "Request parameter has invalid format", request.getRequestURI());
  }
}
