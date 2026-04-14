package com.example.hotelbooking.booking.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record StayPeriod(LocalDate checkIn, LocalDate checkOut) {

  public StayPeriod {
    Objects.requireNonNull(checkIn, "checkIn must not be null");
    Objects.requireNonNull(checkOut, "checkOut must not be null");

    if (!checkOut.isAfter(checkIn)) {
      throw new BookingDomainException("checkOut must be after checkIn");
    }
  }

  public long nights() {
    return ChronoUnit.DAYS.between(checkIn, checkOut);
  }

  public boolean contains(LocalDate date) {
    Objects.requireNonNull(date, "date must not be null");
    return !date.isBefore(checkIn) && date.isBefore(checkOut);
  }
}
