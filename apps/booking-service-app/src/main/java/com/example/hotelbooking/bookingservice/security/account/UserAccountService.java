package com.example.hotelbooking.bookingservice.security.account;

import com.example.hotelbooking.booking.domain.UserId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAccountService {

  private final UserAccountRepository userAccountRepository;

  public UserAccount findOrCreate(
      UserIdentityProvider provider, String providerSubject, String email, String displayName) {
    return userAccountRepository
        .findByProviderAndSubject(provider, providerSubject)
        .orElseGet(() -> create(provider, providerSubject, email, displayName));
  }

  private UserAccount create(
      UserIdentityProvider provider, String providerSubject, String email, String displayName) {
    UserAccount userAccount =
        new UserAccount(
            new UserId(UUID.randomUUID()),
            provider,
            providerSubject,
            email,
            displayName,
            UserAccountRole.USER);

    return userAccountRepository.save(userAccount);
  }
}
