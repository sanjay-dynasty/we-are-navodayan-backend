package com.wearenavodayan.apigateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Correlation-ID";

    private static final Pattern VALID_ID =
            Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = resolveCorrelationId(
                request.getHeader(HEADER_NAME));

        response.setHeader(HEADER_NAME, correlationId);

        HttpServletRequest wrappedRequest =
                new HttpServletRequestWrapper(request) {

                    @Override
                    public String getHeader(String name) {
                        if (HEADER_NAME.equalsIgnoreCase(name)) {
                            return correlationId;
                        }
                        return super.getHeader(name);
                    }

                    @Override
                    public Enumeration<String> getHeaders(String name) {
                        if (HEADER_NAME.equalsIgnoreCase(name)) {
                            return Collections.enumeration(
                                    List.of(correlationId));
                        }
                        return super.getHeaders(name);
                    }

                    @Override
                    public Enumeration<String> getHeaderNames() {
                        List<String> names = Collections.list(
                                super.getHeaderNames());

                        boolean exists = names.stream()
                                .anyMatch(HEADER_NAME::equalsIgnoreCase);

                        if (!exists) {
                            names.add(HEADER_NAME);
                        }

                        return Collections.enumeration(names);
                    }
                };

        filterChain.doFilter(wrappedRequest, response);
    }

    private String resolveCorrelationId(String incomingId) {
        if (incomingId != null && VALID_ID.matcher(incomingId).matches()) {
            return incomingId;
        }

        return UUID.randomUUID().toString();
    }
}