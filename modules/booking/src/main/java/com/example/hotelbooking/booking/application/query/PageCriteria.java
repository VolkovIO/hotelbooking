package com.example.hotelbooking.booking.application.query;

/**
 * Simple application-level pagination request.
 *
 * <p>We intentionally do not use Spring Data Pageable in the booking application module. This keeps
 * the application layer independent from Spring Data and easier to reuse/test.
 */
public record PageCriteria(int page, int size) {

  private static final int MAX_PAGE_SIZE = 100;

  public PageCriteria {
    if (page < 0) {
      throw new IllegalArgumentException("page must not be negative");
    }

    if (size <= 0) {
      throw new IllegalArgumentException("size must be positive");
    }

    if (size > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException("size must not be greater than " + MAX_PAGE_SIZE);
    }
  }

  public long offset() {
    return (long) page * size;
  }
}
