package com.example.hotelbooking.notification.application.service;

import com.example.hotelbooking.notification.application.event.BookingEventEnvelope;
import com.example.hotelbooking.notification.application.event.BookingEventHandlingResult;
import com.example.hotelbooking.notification.application.event.BookingEventRejectedException;
import com.example.hotelbooking.notification.application.port.out.NotificationPreferenceRepository;
import com.example.hotelbooking.notification.application.port.out.NotificationRepository;
import com.example.hotelbooking.notification.domain.Notification;
import com.example.hotelbooking.notification.domain.NotificationBody;
import com.example.hotelbooking.notification.domain.NotificationPreference;
import com.example.hotelbooking.notification.domain.NotificationSubject;
import com.example.hotelbooking.notification.domain.NotificationType;
import com.example.hotelbooking.notification.domain.NotificationUserId;
import com.example.hotelbooking.notification.domain.SourceEventId;
import com.example.hotelbooking.notification.domain.SourceEventType;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingEventNotificationService {

  private static final String BOOKING_CONFIRMED_EVENT = "BookingConfirmed";
  private static final String BOOKING_CANCELLED_EVENT = "BookingCancelled";
  private static final String PAYLOAD_USER_ID = "userId";
  private static final String PREFERENCE_NOT_FOUND = "notification preference was not found";
  private static final String PREFERENCE_DISABLED = "notification preference is disabled";

  private final NotificationRepository notificationRepository;
  private final NotificationPreferenceRepository preferenceRepository;

  public BookingEventHandlingResult handle(BookingEventEnvelope event) {
    Optional<NotificationContent> content = contentFor(event);

    if (content.isEmpty()) {
      return BookingEventHandlingResult.IGNORED_UNSUPPORTED_EVENT;
    }

    Notification notification = createNotification(event, content.get());
    boolean inserted = notificationRepository.insertIfAbsent(notification);

    if (inserted) {
      return BookingEventHandlingResult.CREATED;
    }

    return BookingEventHandlingResult.DUPLICATE;
  }

  private Notification createNotification(
      BookingEventEnvelope event, NotificationContent notificationContent) {
    NotificationUserId userId = new NotificationUserId(requireUserId(event));
    Optional<NotificationPreference> preference = preferenceRepository.findByUserId(userId);

    if (preference.isEmpty()) {
      return skippedNotification(event, notificationContent, userId, PREFERENCE_NOT_FOUND);
    }

    NotificationPreference userPreference = preference.get();

    if (!userPreference.isEnabled()) {
      return skippedNotification(event, notificationContent, userId, PREFERENCE_DISABLED);
    }

    return pendingNotification(event, notificationContent, userPreference);
  }

  private Notification pendingNotification(
      BookingEventEnvelope event,
      NotificationContent notificationContent,
      NotificationPreference preference) {
    return Notification.pending(
        new SourceEventId(event.eventId()),
        new SourceEventType(event.eventType()),
        notificationContent.type(),
        preference.getUserId(),
        preference.getChannel(),
        preference.getDestination(),
        notificationContent.subject(),
        notificationContent.body());
  }

  private Notification skippedNotification(
      BookingEventEnvelope event,
      NotificationContent notificationContent,
      NotificationUserId userId,
      String reason) {
    return Notification.skipped(
        new SourceEventId(event.eventId()),
        new SourceEventType(event.eventType()),
        notificationContent.type(),
        userId,
        notificationContent.subject(),
        notificationContent.body(),
        reason);
  }

  private Optional<NotificationContent> contentFor(BookingEventEnvelope event) {
    if (BOOKING_CONFIRMED_EVENT.equals(event.eventType())) {
      return Optional.of(
          new NotificationContent(
              NotificationType.BOOKING_CONFIRMED,
              new NotificationSubject("Booking confirmed"),
              new NotificationBody("Your booking has been confirmed.")));
    }

    if (BOOKING_CANCELLED_EVENT.equals(event.eventType())) {
      return Optional.of(
          new NotificationContent(
              NotificationType.BOOKING_CANCELLED,
              new NotificationSubject("Booking cancelled"),
              new NotificationBody("Your booking has been cancelled.")));
    }

    return Optional.empty();
  }

  private UUID requireUserId(BookingEventEnvelope event) {
    Object value = event.payload().get(PAYLOAD_USER_ID);

    if (value == null) {
      throw new BookingEventRejectedException("Booking event payload does not contain userId");
    }

    try {
      return UUID.fromString(value.toString());
    } catch (IllegalArgumentException exception) {
      throw new BookingEventRejectedException(
          "Booking event payload contains invalid userId", exception);
    }
  }

  private record NotificationContent(
      NotificationType type, NotificationSubject subject, NotificationBody body) {}
}
