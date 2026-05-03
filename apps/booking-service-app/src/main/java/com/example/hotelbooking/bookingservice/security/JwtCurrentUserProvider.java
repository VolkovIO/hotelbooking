package com.example.hotelbooking.bookingservice.security;

import com.example.hotelbooking.booking.application.security.CurrentUser;
import com.example.hotelbooking.booking.application.security.CurrentUserProvider;
import com.example.hotelbooking.booking.application.security.UserRole;
import com.example.hotelbooking.booking.domain.UserId;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@Profile("security-jwt")
public class JwtCurrentUserProvider implements CurrentUserProvider {

  @Override
  public CurrentUser currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      throw new IllegalStateException("Authenticated JWT principal is required");
    }

    String subject = jwt.getSubject();
    UserId userId = mapSubjectToUserId(subject);

    return new CurrentUser(userId, Set.of(UserRole.USER));
  }

  private UserId mapSubjectToUserId(String subject) {
    UUID userId = UUID.nameUUIDFromBytes(("google:" + subject).getBytes(StandardCharsets.UTF_8));
    return new UserId(userId);
  }
}
