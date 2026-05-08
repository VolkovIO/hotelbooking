package com.example.hotelbooking.notification.adapter.out.persistence.mongo;

import java.time.Instant;
import java.util.Objects;

final class MongoNotificationClaimValidator {

  private MongoNotificationClaimValidator() {}

  static void validateClaimArguments(String lockedBy, Instant now, Instant lockedUntil, int limit) {
    validateLockedBy(lockedBy);
    Objects.requireNonNull(now, "now must not be null");
    Objects.requireNonNull(lockedUntil, "lockedUntil must not be null");

    if (!lockedUntil.isAfter(now)) {
      throw new IllegalArgumentException("lockedUntil must be after now");
    }

    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be positive");
    }
  }

  static void validateLockedBy(String lockedBy) {
    if (lockedBy == null || lockedBy.isBlank()) {
      throw new IllegalArgumentException("lockedBy must not be blank");
    }
  }
}
