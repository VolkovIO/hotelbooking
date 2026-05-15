package com.example.hotelbooking.booking.application.query;

import com.example.hotelbooking.booking.domain.UserId;
import java.util.Objects;

public record GetCurrentUserBookingsQuery(UserId userId, PageCriteria pageCriteria) {

  public GetCurrentUserBookingsQuery {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(pageCriteria, "pageCriteria must not be null");
  }
}
