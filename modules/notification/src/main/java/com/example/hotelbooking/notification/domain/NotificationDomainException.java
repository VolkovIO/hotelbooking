package com.example.hotelbooking.notification.domain;

import java.io.Serial;

public class NotificationDomainException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public NotificationDomainException(String message) {
    super(message);
  }
}
