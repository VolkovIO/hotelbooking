package com.example.hotelbooking.booking.application.query;

import java.util.List;
import java.util.Objects;

/**
 * Application-level paged result.
 *
 * <p>This is a small alternative to Spring Data Page. The booking application module only needs a
 * simple DTO with content and pagination metadata.
 */
public record PagedResult<T>(List<T> content, int page, int size, long totalElements) {

  public PagedResult {
    Objects.requireNonNull(content, "content must not be null");

    if (page < 0) {
      throw new IllegalArgumentException("page must not be negative");
    }

    if (size <= 0) {
      throw new IllegalArgumentException("size must be positive");
    }

    if (totalElements < 0) {
      throw new IllegalArgumentException("totalElements must not be negative");
    }

    content = List.copyOf(content);
  }

  public int totalPages() {
    if (totalElements == 0) {
      return 0;
    }

    return (int) Math.ceil((double) totalElements / size);
  }

  public boolean first() {
    return page == 0;
  }

  public boolean last() {
    int totalPages = totalPages();

    return totalPages == 0 || page >= totalPages - 1;
  }
}
