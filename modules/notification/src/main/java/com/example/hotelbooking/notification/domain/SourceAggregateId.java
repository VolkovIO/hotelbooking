package com.example.hotelbooking.notification.domain;

import java.util.Objects;
import java.util.UUID;

public record SourceAggregateId(UUID value) {

  public SourceAggregateId {
    Objects.requireNonNull(value, "source aggregate id must not be null");
  }
}
