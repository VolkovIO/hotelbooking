package com.example.hotelbooking.observability.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdMdcFilterTest {

  private final CorrelationIdMdcFilter filter = new CorrelationIdMdcFilter();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void createsCorrelationIdWhenHeaderIsAbsent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> mdcCorrelationId = new AtomicReference<>();

    filter.doFilter(request, response, filterChainThatReadsMdc(mdcCorrelationId));

    assertThat(mdcCorrelationId.get()).isNotBlank();
    assertThat(response.getHeader(CorrelationIdMdcFilter.CORRELATION_ID_HEADER))
        .isEqualTo(mdcCorrelationId.get());
    assertThat(MDC.get(CorrelationIdMdcFilter.CORRELATION_ID_MDC_KEY)).isNull();
  }

  @Test
  void reusesSafeCorrelationIdHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings");
    request.addHeader(CorrelationIdMdcFilter.CORRELATION_ID_HEADER, "demo-correlation-123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> mdcCorrelationId = new AtomicReference<>();

    filter.doFilter(request, response, filterChainThatReadsMdc(mdcCorrelationId));

    assertThat(mdcCorrelationId.get()).isEqualTo("demo-correlation-123");
    assertThat(response.getHeader(CorrelationIdMdcFilter.CORRELATION_ID_HEADER))
        .isEqualTo("demo-correlation-123");
  }

  @Test
  void replacesUnsafeCorrelationIdHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings");
    request.addHeader(CorrelationIdMdcFilter.CORRELATION_ID_HEADER, "bad value with spaces");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> mdcCorrelationId = new AtomicReference<>();

    filter.doFilter(request, response, filterChainThatReadsMdc(mdcCorrelationId));

    assertThat(mdcCorrelationId.get()).isNotEqualTo("bad value with spaces");
    assertThat(mdcCorrelationId.get()).isNotBlank();
  }

  private MockFilterChain filterChainThatReadsMdc(AtomicReference<String> mdcCorrelationId) {
    return new MockFilterChain() {
      @Override
      public void doFilter(ServletRequest request, ServletResponse response)
          throws IOException, ServletException {
        mdcCorrelationId.set(MDC.get(CorrelationIdMdcFilter.CORRELATION_ID_MDC_KEY));
        super.doFilter(request, response);
      }
    };
  }
}
