package com.example.hotelbooking.inventoryservice.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

final class InventorySecurityRules {

  private InventorySecurityRules() {}

  static void configure(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          authorization) {
    authorization
        .requestMatchers(
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml")
        .permitAll()
        .requestMatchers("/api/v1/admin/**")
        .hasRole("ADMIN")
        .anyRequest()
        .permitAll();
  }
}
