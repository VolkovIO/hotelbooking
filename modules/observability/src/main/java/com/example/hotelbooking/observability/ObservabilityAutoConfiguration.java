package com.example.hotelbooking.observability;

import com.example.hotelbooking.observability.grpc.MdcGrpcClientInterceptor;
import com.example.hotelbooking.observability.grpc.MdcGrpcServerInterceptor;
import com.example.hotelbooking.observability.web.CorrelationIdMdcFilter;
import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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

  @Bean
  @ConditionalOnClass(ClientInterceptor.class)
  @ConditionalOnMissingBean(name = "hotelbookingGrpcClientObservabilityInterceptor")
  ClientInterceptor hotelbookingGrpcClientObservabilityInterceptor() {
    return new MdcGrpcClientInterceptor();
  }

  @Bean
  @ConditionalOnClass(ServerInterceptor.class)
  @ConditionalOnMissingBean(name = "hotelbookingGrpcServerObservabilityInterceptor")
  ServerInterceptor hotelbookingGrpcServerObservabilityInterceptor() {
    return new MdcGrpcServerInterceptor();
  }
}
