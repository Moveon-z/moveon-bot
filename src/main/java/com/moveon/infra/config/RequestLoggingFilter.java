package com.moveon.infra.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Filter for logging HTTP requests and responses.
 * Logs request method, path, headers, and response status with duration.
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String START_TIME_ATTR = "startTime";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        Instant startTime = Instant.now();
        wrappedRequest.setAttribute(START_TIME_ATTR, startTime);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            logRequest(wrappedRequest, wrappedResponse, startTime);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            Instant startTime
    ) {
        long duration = java.time.Duration.between(startTime, Instant.now()).toMillis();

        String requestBody = getRequestBody(request);
        String responseBody = getResponseBody(response);

        log.info("HTTP {} {} - Status: {} - Duration: {}ms - Request: {} - Response: {}",
                request.getMethod(),
                getRequestUri(request),
                response.getStatus(),
                duration,
                truncate(requestBody, 500),
                truncate(responseBody, 500));
    }

    private String getRequestUri(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String requestUri = request.getRequestURI();
        return queryString != null ? requestUri + "?" + queryString : requestUri;
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "... (" + value.length() + " bytes)";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip logging for static resources, health checks, and SSE streams
        String path = request.getServletPath();
        if (path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs") ||
            path.startsWith("/actuator") ||
            path.equals("/health")) {
            return true;
        }
        // Skip for SSE streaming endpoints (ContentCachingResponseWrapper buffers output)
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/event-stream");
    }
}
