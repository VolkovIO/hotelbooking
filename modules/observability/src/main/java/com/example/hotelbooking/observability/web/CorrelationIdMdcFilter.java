package com.example.hotelbooking.observability.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds a correlation id to the logging MDC for every incoming HTTP request.
 *
 * <p>The project already propagates {@code correlationId} through booking/payment domain events.
 * This filter solves a different problem: it makes the same kind of identifier visible in
 * application logs, including logs produced before a request reaches a controller or when a request
 * fails in a framework/security layer.
 *
 * <p>Behaviour:
 *
 * <ul>
 *   <li>If the client sends {@code X-Correlation-Id}, the filter reuses it when it is safe for
 *       logs.
 *   <li>If the header is absent or unsafe, the filter creates a new UUID value.
 *   <li>The effective value is written to the response header so that a client can copy it into bug
 *       reports or follow-up calls.
 *   <li>The MDC entry is removed after request processing to prevent correlation id leakage between
 *       requests handled by the same servlet thread.
 * </ul>
 */
public final class CorrelationIdMdcFilter extends OncePerRequestFilter {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
  public static final String CORRELATION_ID_MDC_KEY = "correlationId";

  private static final Pattern SAFE_CORRELATION_ID_PATTERN =
      Pattern.compile("[A-Za-z0-9._:-]{1,128}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String correlationId = resolveCorrelationId(request.getHeader(CORRELATION_ID_HEADER));
    response.setHeader(CORRELATION_ID_HEADER, correlationId);

    try (MDC.MDCCloseable ignored = MDC.putCloseable(CORRELATION_ID_MDC_KEY, correlationId)) {
      filterChain.doFilter(request, response);
    }
  }

  private String resolveCorrelationId(String headerValue) {
    if (headerValue == null) {
      return newCorrelationId();
    }

    String candidate = headerValue.trim();
    if (SAFE_CORRELATION_ID_PATTERN.matcher(candidate).matches()) {
      return candidate;
    }

    return newCorrelationId();
  }

  private String newCorrelationId() {
    return UUID.randomUUID().toString();
  }
}
