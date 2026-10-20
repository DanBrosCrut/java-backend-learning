package com.example.moysklad.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that assigns a correlation ID to every request.
 * Reads the {@code X-Correlation-ID} header if present, otherwise generates a new UUID.
 * The ID is stored in the MDC under {@code correlationId} so it can be included in log output,
 * and is exposed back to the client via the response header.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Name of the HTTP header carrying the correlation ID. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    /** MDC key under which the correlation ID is available to the logging pattern. */
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    /**
     * Puts the correlation ID into the MDC, runs the filter chain, and clears the MDC afterwards.
     *
     * @param request     the current request
     * @param response    the current response
     * @param filterChain the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        try {
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
