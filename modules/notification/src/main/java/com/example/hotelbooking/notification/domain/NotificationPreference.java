package com.example.hotelbooking.notification.domain;

import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

@Getter
public class NotificationPreference {

  private final NotificationUserId userId;
  private NotificationChannel channel;
  private NotificationDestination destination;
  private boolean enabled;
  private final Instant createdAt;
  private Instant updatedAt;

  private NotificationPreference(
      NotificationUserId userId,
      NotificationChannel channel,
      NotificationDestination destination,
      boolean enabled,
      Instant createdAt,
      Instant updatedAt) {
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.channel = Objects.requireNonNull(channel, "channel must not be null");
    this.destination = Objects.requireNonNull(destination, "destination must not be null");
    this.enabled = enabled;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
  }

  public static NotificationPreference create(
      NotificationUserId userId,
      NotificationChannel channel,
      NotificationDestination destination,
      boolean enabled) {
    Instant now = Instant.now();

    return new NotificationPreference(userId, channel, destination, enabled, now, now);
  }

  public static NotificationPreference restore(
      NotificationUserId userId,
      NotificationChannel channel,
      NotificationDestination destination,
      boolean enabled,
      Instant createdAt,
      Instant updatedAt) {
    return new NotificationPreference(userId, channel, destination, enabled, createdAt, updatedAt);
  }

  public void update(
      NotificationChannel channel, NotificationDestination destination, boolean enabled) {
    this.channel = Objects.requireNonNull(channel, "channel must not be null");
    this.destination = Objects.requireNonNull(destination, "destination must not be null");
    this.enabled = enabled;
    this.updatedAt = Instant.now();
  }
}
