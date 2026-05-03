package com.example.hotelbooking.booking.application.security;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface CurrentUserProvider {

  CurrentUser currentUser();
}
