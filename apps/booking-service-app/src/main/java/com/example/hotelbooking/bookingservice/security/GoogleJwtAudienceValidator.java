package com.example.hotelbooking.bookingservice.security;

import java.util.List;
import java.util.Objects;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

final class GoogleJwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

  private static final String ERROR_CODE = "invalid_token";

  private final String expectedAudience;

  GoogleJwtAudienceValidator(String expectedAudience) {
    this.expectedAudience =
        Objects.requireNonNull(expectedAudience, "expectedAudience must not be null");
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    List<String> audiences = jwt.getAudience();

    if (audiences.contains(expectedAudience)) {
      return OAuth2TokenValidatorResult.success();
    }

    OAuth2Error error =
        new OAuth2Error(
            ERROR_CODE, "JWT audience does not contain expected Google OAuth client id", null);

    return OAuth2TokenValidatorResult.failure(error);
  }
}
