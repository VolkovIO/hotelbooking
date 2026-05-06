package com.example.hotelbooking.notification.domain;

import java.util.Objects;
import java.util.UUID;

public record SourceEventId(UUID value) {

  public SourceEventId {
    Objects.requireNonNull(value, "source event id must not be null");
  }
}
