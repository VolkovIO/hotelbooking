package com.example.hotelbooking.audit.application.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AuditClockConfiguration {

  @Bean
  Clock auditClock() {
    return Clock.systemUTC();
  }
}
