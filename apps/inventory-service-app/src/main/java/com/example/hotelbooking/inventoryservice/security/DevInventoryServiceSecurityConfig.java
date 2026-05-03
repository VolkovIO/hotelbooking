package com.example.hotelbooking.inventoryservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Profile("security-dev")
@RequiredArgsConstructor
public class DevInventoryServiceSecurityConfig {

  private final DevSecurityAuthenticationFilter devSecurityAuthenticationFilter;

  @Bean
  SecurityFilterChain inventorySecurityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .addFilterBefore(
            devSecurityAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(InventorySecurityRules::configure)
        .build();
  }
}
