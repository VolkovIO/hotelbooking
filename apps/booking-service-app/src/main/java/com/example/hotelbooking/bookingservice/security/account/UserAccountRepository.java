package com.example.hotelbooking.bookingservice.security.account;

import java.util.Optional;

public interface UserAccountRepository {

  Optional<UserAccount> findByProviderAndSubject(
      UserIdentityProvider provider, String providerSubject);

  UserAccount save(UserAccount userAccount);
}
