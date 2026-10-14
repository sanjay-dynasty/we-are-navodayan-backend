package com.wearenavodayan.apigateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long CLEANUP_INTERVAL = 256;

    private final int capacity;
    private final long windowMillis;

    private final ConcurrentHashMap<String, RequestWindow> clients =
            new ConcurrentHashMap<>();

    private final AtomicLong requestSequence = new AtomicLong();

    public RateLimitFilter(
            @Value("${gateway.rate-limit.capacity:100}") int capacity,
            @Value("${gateway.rate-limit.window-seconds:60}") long windowSeconds) {

        if (capacity < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException(
                    "Rate-limit capacity and window must be positive");
        }

        this.capacity = capacity;
        this.windowMillis = windowSeconds * 1000L;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long now = System.currentTimeMillis();

        // Periodically remove expired client windows.
        if (requestSequence.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            cleanupExpiredWindows(now);
        }

        String clientIp = request.getRemoteAddr();

        RequestWindow window = clients.compute(clientIp, (key, current) -> {
            if (current == null
                    || now - current.startedAt >= windowMillis) {
                return new RequestWindow(now, new AtomicInteger(0));
            }
            return current;
        });

        int requestCount = window.count.incrementAndGet();

        response.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(Math.max(0, capacity - requestCount)));

        if (requestCount > capacity) {
            long elapsed = now - window.startedAt;
            long remainingMillis = Math.max(1, windowMillis - elapsed);
            long retryAfterSeconds = (remainingMillis + 999) / 1000;

            response.setHeader(
                    "Retry-After",
                    String.valueOf(retryAfterSeconds));
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\","
                            + "\"message\":\"Rate limit exceeded. Try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void cleanupExpiredWindows(long now) {
        clients.entrySet().removeIf(entry ->
                now - entry.getValue().startedAt >= windowMillis);
    }

    private static final class RequestWindow {
        private final long startedAt;
        private final AtomicInteger count;

        private RequestWindow(long startedAt, AtomicInteger count) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }
}