package com.example.hotelbooking.bookingservice.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

final class BookingSecurityRules {

  private BookingSecurityRules() {}

  static void configure(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          authorization) {
    authorization
        .requestMatchers(
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml")
        .permitAll()
        .requestMatchers(HttpMethod.POST, "/api/v1/bookings")
        .hasAnyRole("USER", "ADMIN")
        .requestMatchers("/api/v1/bookings/**")
        .hasAnyRole("USER", "ADMIN")
        .anyRequest()
        .permitAll();
  }
}
