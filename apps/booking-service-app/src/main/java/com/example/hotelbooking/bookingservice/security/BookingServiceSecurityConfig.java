package com.example.hotelbooking.bookingservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class BookingServiceSecurityConfig {

  private final ObjectProvider<DevSecurityAuthenticationFilter> devSecurityAuthenticationFilter;

  @Bean
  SecurityFilterChain bookingSecurityFilterChain(HttpSecurity http) throws Exception {
    devSecurityAuthenticationFilter.ifAvailable(
        filter -> http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));

    return http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/bookings")
                    .hasAnyRole("USER", "ADMIN")
                    .requestMatchers("/api/v1/bookings/**")
                    .hasAnyRole("USER", "ADMIN")
                    .anyRequest()
                    .permitAll())
        .build();
  }
}
