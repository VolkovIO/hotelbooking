package com.example.hotelbooking.bookingservice.security.account;

import com.example.hotelbooking.booking.domain.UserId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcUserAccountRepository implements UserAccountRepository {

  private final JdbcClient jdbcClient;

  @Override
  public Optional<UserAccount> findByProviderAndSubject(
      UserIdentityProvider provider, String providerSubject) {
    return jdbcClient
        .sql(
            """
            select id,
                   provider,
                   provider_subject,
                   email,
                   display_name,
                   role
              from app_users
             where provider = :provider
               and provider_subject = :providerSubject
            """)
        .param("provider", provider.name())
        .param("providerSubject", providerSubject)
        .query(this::mapRow)
        .optional();
  }

  @Override
  public UserAccount save(UserAccount userAccount) {
    jdbcClient
        .sql(
            """
            insert into app_users (
                id,
                provider,
                provider_subject,
                email,
                display_name,
                role
            )
            values (
                :id,
                :provider,
                :providerSubject,
                :email,
                :displayName,
                :role
            )
            """)
        .param("id", userAccount.id().value())
        .param("provider", userAccount.provider().name())
        .param("providerSubject", userAccount.providerSubject())
        .param("email", userAccount.email())
        .param("displayName", userAccount.displayName())
        .param("role", userAccount.role().name())
        .update();

    return userAccount;
  }

  @SuppressWarnings("unused")
  private UserAccount mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
    return new UserAccount(
        new UserId(resultSet.getObject("id", UUID.class)),
        UserIdentityProvider.valueOf(resultSet.getString("provider")),
        resultSet.getString("provider_subject"),
        resultSet.getString("email"),
        resultSet.getString("display_name"),
        UserAccountRole.valueOf(resultSet.getString("role")));
  }
}
