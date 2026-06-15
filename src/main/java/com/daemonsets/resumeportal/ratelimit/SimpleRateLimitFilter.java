package com.daemonsets.resumeportal.ratelimit;

import com.daemonsets.resumeportal.web.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class SimpleRateLimitFilter extends OncePerRequestFilter {
    private static final long CLEANUP_EVERY_REQUESTS = 1_000;

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();

    public SimpleRateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Limit limit = resolveLimit(request);
        if (!properties.isEnabled() || limit == null || limit.capacity() <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        long windowMillis = Math.max(1_000L, properties.getWindow().toMillis());
        String key = limit.name() + ":" + clientIp(request);
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(now));

        if (!bucket.tryConsume(now, windowMillis, limit.capacity())) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of(
                    "Too many requests. Please retry shortly.",
                    429,
                    request.getRequestURI()
            ));
            return;
        }

        cleanupExpiredBuckets(now, windowMillis);
        filterChain.doFilter(request, response);
    }

    private Limit resolveLimit(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path == null || !path.startsWith("/api/")) {
            return null;
        }
        if (path.equals("/api/csrf")) {
            return null;
        }
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
            return new Limit("auth", properties.getLoginCapacity());
        }
        if (path.equals("/api/profile/export/pdf") && HttpMethod.GET.matches(method)) {
            return new Limit("pdf", properties.getPdfCapacity());
        }
        if (path.startsWith("/api/public/")) {
            return new Limit("public", properties.getPublicCapacity());
        }
        return new Limit("api", properties.getApiCapacity());
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private void cleanupExpiredBuckets(long now, long windowMillis) {
        if (requestCounter.incrementAndGet() % CLEANUP_EVERY_REQUESTS != 0) {
            return;
        }
        buckets.entrySet().removeIf(entry -> entry.getValue().isExpired(now, windowMillis));
    }

    private record Limit(String name, int capacity) {
    }

    private static class Bucket {
        private long windowStartMillis;
        private int count;

        private Bucket(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
        }

        private synchronized boolean tryConsume(long now, long windowMillis, int capacity) {
            if (now - windowStartMillis >= windowMillis) {
                windowStartMillis = now;
                count = 0;
            }
            if (count >= capacity) {
                return false;
            }
            count++;
            return true;
        }

        private synchronized boolean isExpired(long now, long windowMillis) {
            return now - windowStartMillis > windowMillis * 2;
        }
    }
}
