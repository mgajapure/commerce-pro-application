package com.commerce_pro_backend.user_identity.service;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * IP-level rate limiter for auth endpoints (/auth/login and /auth/refresh).
 * Uses a sliding window per IP address.
 *
 * Limits: max requests per window period per IP.
 * Configurable via application properties.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.auth.max-requests:20}")
    private int maxRequests;

    @Value("${app.rate-limit.auth.window-seconds:60}")
    private int windowSeconds;

    // Key: IP address, Value: request count in current window
    private final ConcurrentHashMap<String, RateLimitEntry> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Only rate-limit auth endpoints
        return !path.startsWith("/api/v1/auth/login") && !path.startsWith("/api/v1/auth/refresh");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = extractClientIp(request);
        RateLimitEntry entry = requestCounts.computeIfAbsent(clientIp, k -> new RateLimitEntry());

        if (!entry.tryConsume(maxRequests, windowSeconds)) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, request.getServletPath());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            response.getWriter().write(
                "{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Purge stale entries every 5 minutes to prevent unbounded memory growth.
     */
    @Scheduled(fixedDelay = 300_000)
    public void purgeExpiredEntries() {
        long now = Instant.now().getEpochSecond();
        int removed = 0;
        for (var iter = requestCounts.entrySet().iterator(); iter.hasNext(); ) {
            var entry = iter.next();
            if (entry.getValue().isWindowExpired(now, windowSeconds)) {
                iter.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Rate limiter purged {} stale IP entries", removed);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        // Respect X-Forwarded-For when behind a proxy/load balancer
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // -------------------------------------------------------------------------
    // Inner class: sliding window counter per IP
    // -------------------------------------------------------------------------

    private static class RateLimitEntry {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = Instant.now().getEpochSecond();

        synchronized boolean tryConsume(int maxRequests, int windowSeconds) {
            long now = Instant.now().getEpochSecond();
            if (now - windowStart >= windowSeconds) {
                // New window — reset
                count.set(0);
                windowStart = now;
            }
            return count.incrementAndGet() <= maxRequests;
        }

        boolean isWindowExpired(long now, int windowSeconds) {
            return now - windowStart >= windowSeconds * 2L;
        }
    }
}
