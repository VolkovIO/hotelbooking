package com.example.hotelbooking.booking.application.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PageCriteriaTest {

  @Test
  void shouldCalculateOffset() {
    PageCriteria pageCriteria = new PageCriteria(2, 20);

    assertEquals(40, pageCriteria.offset());
  }

  @Test
  void shouldRejectNegativePage() {
    assertThrows(IllegalArgumentException.class, () -> new PageCriteria(-1, 20));
  }

  @Test
  void shouldRejectNonPositiveSize() {
    assertThrows(IllegalArgumentException.class, () -> new PageCriteria(0, 0));
  }

  @Test
  void shouldRejectTooLargeSize() {
    assertThrows(IllegalArgumentException.class, () -> new PageCriteria(0, 101));
  }
}
