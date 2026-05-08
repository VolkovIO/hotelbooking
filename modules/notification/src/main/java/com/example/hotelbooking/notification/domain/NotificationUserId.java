package com.example.hotelbooking.notification.domain;

import java.util.Objects;
import java.util.UUID;

public record NotificationUserId(UUID value) {

  public NotificationUserId {
    Objects.requireNonNull(value, "notification user id must not be null");
  }
}
