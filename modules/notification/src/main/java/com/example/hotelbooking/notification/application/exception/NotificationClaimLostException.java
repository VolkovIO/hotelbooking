package com.example.hotelbooking.notification.application.exception;

import com.example.hotelbooking.notification.domain.NotificationId;

public class NotificationClaimLostException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public NotificationClaimLostException(NotificationId notificationId) {
    super("Notification claim was lost: " + notificationId.value());
  }
}
