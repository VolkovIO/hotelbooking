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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@Profile("security-jwt")
@RequiredArgsConstructor
public class JwtCurrentUserProvider implements CurrentUserProvider {

  private final UserAccountService userAccountService;

  @Override
  public CurrentUser currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      throw new IllegalStateException("Authenticated JWT principal is required");
    }

    UserAccount userAccount =
        userAccountService.findOrCreate(
            UserIdentityProvider.GOOGLE,
            jwt.getSubject(),
            jwt.getClaimAsString("email"),
            jwt.getClaimAsString("name"));

    return new CurrentUser(userAccount.id(), mapRoles(userAccount));
  }

  private Set<UserRole> mapRoles(UserAccount userAccount) {
    return switch (userAccount.role()) {
      case ADMIN -> Set.of(UserRole.USER, UserRole.ADMIN);
      case USER -> Set.of(UserRole.USER);
    };
  }
}
