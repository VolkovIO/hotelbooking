package com.example.hotelbooking.notification.domain;

import java.util.Objects;
import java.util.UUID;

public record SourceCorrelationId(UUID value) {

  public SourceCorrelationId {
    Objects.requireNonNull(value, "source correlation id must not be null");
  }
}
