package com.example.hotelbooking.inventory.adapter.in.web;

import com.example.hotelbooking.inventory.application.exception.HotelNotFoundException;
import com.example.hotelbooking.inventory.application.exception.RoomAvailabilityNotFoundException;
import com.example.hotelbooking.inventory.application.exception.RoomHoldNotFoundException;
import com.example.hotelbooking.inventory.domain.InventoryDomainException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
    basePackageClasses = {InventoryAdminController.class, PublicInventoryController.class})
public class InventoryExceptionHandler {

  @ExceptionHandler(HotelNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public InventoryApiErrorResponse handleHotelNotFound(HotelNotFoundException exception) {
    return InventoryApiErrorResponse.of(
        HttpStatus.NOT_FOUND.value(),
        HttpStatus.NOT_FOUND.getReasonPhrase(),
        exception.getMessage());
  }

  @ExceptionHandler(RoomAvailabilityNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public InventoryApiErrorResponse handleRoomAvailabilityNotFound(
      RoomAvailabilityNotFoundException exception) {
    return InventoryApiErrorResponse.of(
        HttpStatus.NOT_FOUND.value(),
        HttpStatus.NOT_FOUND.getReasonPhrase(),
        exception.getMessage());
  }

  @ExceptionHandler(RoomHoldNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public InventoryApiErrorResponse handleRoomHoldNotFound(RoomHoldNotFoundException exception) {
    return InventoryApiErrorResponse.of(
        HttpStatus.NOT_FOUND.value(),
        HttpStatus.NOT_FOUND.getReasonPhrase(),
        exception.getMessage());
  }

  @ExceptionHandler(InventoryDomainException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public InventoryApiErrorResponse handleInventoryDomain(InventoryDomainException exception) {
    return InventoryApiErrorResponse.of(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public InventoryApiErrorResponse handleValidation(MethodArgumentNotValidException exception) {
    List<InventoryValidationError> validationErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new InventoryValidationError(error.getField(), error.getDefaultMessage()))
            .toList();

    return InventoryApiErrorResponse.validationError(validationErrors);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public InventoryApiErrorResponse handleConstraintViolation(
      ConstraintViolationException exception) {
    List<InventoryValidationError> validationErrors =
        exception.getConstraintViolations().stream()
            .map(
                violation ->
                    new InventoryValidationError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();

    return InventoryApiErrorResponse.validationError(validationErrors);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public InventoryApiErrorResponse handleIllegalArgument(IllegalArgumentException exception) {
    return InventoryApiErrorResponse.of(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        exception.getMessage());
  }
}
