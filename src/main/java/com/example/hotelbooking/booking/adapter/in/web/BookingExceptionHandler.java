package com.example.hotelbooking.booking.adapter.in.web;

import com.example.hotelbooking.booking.application.exception.BookingNotFoundException;
import com.example.hotelbooking.booking.application.exception.RoomHoldFailedException;
import com.example.hotelbooking.booking.domain.BookingDomainException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = BookingController.class)
public class BookingExceptionHandler {

  @ExceptionHandler(BookingNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public BookingApiErrorResponse handleBookingNotFound(BookingNotFoundException exception) {
    return BookingApiErrorResponse.of(
        HttpStatus.NOT_FOUND.value(),
        HttpStatus.NOT_FOUND.getReasonPhrase(),
        exception.getMessage());
  }

  @ExceptionHandler(RoomHoldFailedException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public BookingApiErrorResponse handleRoomHoldFailed(RoomHoldFailedException exception) {
    return BookingApiErrorResponse.of(
        HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT.getReasonPhrase(), exception.getMessage());
  }

  @ExceptionHandler(BookingDomainException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public BookingApiErrorResponse handleBookingDomain(BookingDomainException exception) {
    return BookingApiErrorResponse.of(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public BookingApiErrorResponse handleValidation(MethodArgumentNotValidException exception) {
    List<BookingValidationError> validationErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new BookingValidationError(error.getField(), error.getDefaultMessage()))
            .toList();

    return BookingApiErrorResponse.validationError(validationErrors);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public BookingApiErrorResponse handleConstraintViolation(ConstraintViolationException exception) {
    List<BookingValidationError> validationErrors =
        exception.getConstraintViolations().stream()
            .map(
                violation ->
                    new BookingValidationError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();

    return BookingApiErrorResponse.validationError(validationErrors);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public BookingApiErrorResponse handleIllegalArgument(IllegalArgumentException exception) {
    return BookingApiErrorResponse.of(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        exception.getMessage());
  }
}
