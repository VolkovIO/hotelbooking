package com.example.hotelbooking.common.api;

import com.example.hotelbooking.booking.application.exception.BookingNotFoundException;
import com.example.hotelbooking.booking.application.exception.HotelReferenceNotFoundException;
import com.example.hotelbooking.booking.application.exception.RoomTypeNotAvailableException;
import com.example.hotelbooking.booking.application.exception.RoomTypeReferenceNotFoundException;
import com.example.hotelbooking.booking.domain.BookingDomainException;
import com.example.hotelbooking.inventory.application.exception.HotelNotFoundException;
import com.example.hotelbooking.inventory.domain.InventoryDomainException;
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

  @ExceptionHandler(HotelNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse handleHotelNotFoundException(
      HotelNotFoundException exception, HttpServletRequest request) {
    return ApiErrorResponse.of("HOTEL_NOT_FOUND", exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(InventoryDomainException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleInventoryDomainException(
      InventoryDomainException exception, HttpServletRequest request) {
    return ApiErrorResponse.of(
        "INVENTORY_DOMAIN_ERROR", exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(HotelReferenceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse handleHotelReferenceNotFoundException(
      HotelReferenceNotFoundException exception, HttpServletRequest request) {
    return ApiErrorResponse.of(
        "HOTEL_REFERENCE_NOT_FOUND", exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(RoomTypeReferenceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse handleRoomTypeReferenceNotFoundException(
      RoomTypeReferenceNotFoundException exception, HttpServletRequest request) {
    return ApiErrorResponse.of(
        "ROOM_TYPE_REFERENCE_NOT_FOUND", exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(RoomTypeNotAvailableException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiErrorResponse handleRoomTypeNotAvailableException(
      RoomTypeNotAvailableException exception, HttpServletRequest request) {
    return ApiErrorResponse.of(
        "ROOM_TYPE_NOT_AVAILABLE", exception.getMessage(), request.getRequestURI());
  }
}
