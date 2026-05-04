package com.example.hotelbooking.booking.application.security;

import com.example.hotelbooking.booking.domain.UserId;
import java.util.Set;

public record CurrentUser(UserId userId, Set<UserRole> roles) {

  public CurrentUser {
    roles = Set.copyOf(roles);
  }

  public boolean hasRole(UserRole role) {
    return roles.contains(role);
  }
}
