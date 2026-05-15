package com.example.hotelbooking.booking.application.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PagedResultTest {

  @Test
  void shouldCalculateTotalPages() {
    PagedResult<String> result = new PagedResult<>(List.of("a", "b"), 0, 20, 41);

    assertEquals(3, result.totalPages());
  }

  @Test
  void shouldDetectFirstPage() {
    PagedResult<String> result = new PagedResult<>(List.of("a"), 0, 20, 1);

    assertTrue(result.first());
  }

  @Test
  void shouldDetectLastPage() {
    PagedResult<String> result = new PagedResult<>(List.of("a"), 2, 20, 41);

    assertTrue(result.last());
  }

  @Test
  void shouldDetectNonLastPage() {
    PagedResult<String> result = new PagedResult<>(List.of("a"), 0, 20, 41);

    assertFalse(result.last());
  }

  @Test
  void shouldRejectNullContent() {
    assertThrows(NullPointerException.class, () -> new PagedResult<>(null, 0, 20, 0));
  }
}
