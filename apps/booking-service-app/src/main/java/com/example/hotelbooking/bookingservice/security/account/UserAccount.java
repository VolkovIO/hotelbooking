package com.example.hotelbooking.bookingservice.security.account;

import com.example.hotelbooking.booking.domain.UserId;
import java.util.Objects;

public record UserAccount(
    UserId id,
    UserIdentityProvider provider,
    String providerSubject,
    String email,
    String displayName,
    UserAccountRole role) {

  public UserAccount {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(provider, "provider must not be null");
    Objects.requireNonNull(providerSubject, "providerSubject must not be null");
    Objects.requireNonNull(role, "role must not be null");
  }
}
