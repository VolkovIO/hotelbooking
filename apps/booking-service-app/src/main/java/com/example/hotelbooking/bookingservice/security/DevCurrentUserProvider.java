package com.example.hotelbooking.bookingservice.security;

import com.example.hotelbooking.booking.application.security.CurrentUser;
import com.example.hotelbooking.booking.application.security.CurrentUserProvider;
import com.example.hotelbooking.booking.application.security.UserRole;
import com.example.hotelbooking.booking.domain.UserId;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("security-dev")
public class DevCurrentUserProvider implements CurrentUserProvider {

  private static final UserId DEV_USER_ID =
      new UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

  @Override
  public CurrentUser currentUser() {
    return new CurrentUser(DEV_USER_ID, Set.of(UserRole.USER, UserRole.ADMIN));
  }
}
