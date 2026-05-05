package com.example.hotelbooking.booking.application.event;

public enum BookingOutboxStatus {
  NEW,
  PROCESSING,
  PUBLISHED,
  FAILED
}
