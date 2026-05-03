package com.example.hotelbooking.bookingservice.security;

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
public class DevBookingServiceSecurityConfig {

  private final DevSecurityAuthenticationFilter devSecurityAuthenticationFilter;

  @Bean
  SecurityFilterChain bookingSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .addFilterBefore(
            devSecurityAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(BookingSecurityRules::configure)
        .build();
  }
}
