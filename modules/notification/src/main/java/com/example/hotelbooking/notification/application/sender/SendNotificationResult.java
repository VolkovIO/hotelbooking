package com.example.hotelbooking.notification.application.sender;

public record SendNotificationResult(
    boolean success, String providerMessageId, String errorMessage) {

  public static SendNotificationResult success(String providerMessageId) {
    return new SendNotificationResult(true, providerMessageId, null);
  }

  public static SendNotificationResult failure(String errorMessage) {
    return new SendNotificationResult(false, null, errorMessage);
  }
}
