package com.example.hotelbooking.notification.adapter.in.web.preference;

import com.example.hotelbooking.notification.domain.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record UpdateNotificationPreferenceRequest(
    @NotNull NotificationChannel channel, @NotBlank String destination, boolean enabled) {}
