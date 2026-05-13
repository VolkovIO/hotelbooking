package com.example.hotelbooking.observability;

import com.example.hotelbooking.observability.web.CorrelationIdMdcFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
public class ObservabilityAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(CorrelationIdMdcFilter.class)
  FilterRegistrationBean<CorrelationIdMdcFilter> correlationIdMdcFilterRegistration() {
    FilterRegistrationBean<CorrelationIdMdcFilter> registration =
        new FilterRegistrationBean<>(new CorrelationIdMdcFilter());

    registration.setName("correlationIdMdcFilter");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.addUrlPatterns("/*");

    return registration;
  }
}
