package com.example.hotelbooking.bookingservice.security;

import com.example.hotelbooking.booking.application.security.CurrentUser;
import com.example.hotelbooking.booking.application.security.CurrentUserProvider;
import com.example.hotelbooking.booking.application.security.UserRole;
import com.example.hotelbooking.bookingservice.security.account.UserAccount;
import com.example.hotelbooking.bookingservice.security.account.UserAccountService;
import com.example.hotelbooking.bookingservice.security.account.UserIdentityProvider;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("security-dev")
@RequiredArgsConstructor
public class DevCurrentUserProvider implements CurrentUserProvider {

  private final UserAccountService userAccountService;

  @Override
  public CurrentUser currentUser() {
    UserAccount userAccount =
        userAccountService.findOrCreate(
            UserIdentityProvider.DEV, "dev-user", "dev@example.com", "Development User");

    return new CurrentUser(userAccount.id(), Set.of(UserRole.USER, UserRole.ADMIN));
  }
}
