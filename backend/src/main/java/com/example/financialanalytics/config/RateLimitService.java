package com.example.financialanalytics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
final class RateLimitService {
    private static final long CLEANUP_EVERY_REQUESTS = 1024;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong requestsUntilCleanup = new AtomicLong(CLEANUP_EVERY_REQUESTS);
    private final Clock clock;
    private final int windowSeconds;
    private final int apiRequestsPerWindow;
    private final int authRequestsPerWindow;

    RateLimitService(Clock clock,
                     @Value("${app.rate-limit.window-seconds:60}") int windowSeconds,
                     @Value("${app.rate-limit.api-requests:120}") int apiRequestsPerWindow,
                     @Value("${app.rate-limit.auth-requests:10}") int authRequestsPerWindow) {
        if (windowSeconds < 1 || apiRequestsPerWindow < 1 || authRequestsPerWindow < 1) {
            throw new IllegalArgumentException("Rate limit settings must be positive");
        }
        this.clock = clock;
        this.windowSeconds = windowSeconds;
        this.apiRequestsPerWindow = apiRequestsPerWindow;
        this.authRequestsPerWindow = authRequestsPerWindow;
    }

    Decision acquire(String key, boolean authenticationEndpoint) {
        long now = clock.instant().getEpochSecond();
        long windowStart = now - now % windowSeconds;
        int limit = authenticationEndpoint ? authRequestsPerWindow : apiRequestsPerWindow;
        Window updated = windows.compute(key, (ignored, current) -> {
            if (current == null || current.start() != windowStart) {
                return new Window(windowStart, 1);
            }
            return new Window(windowStart, current.count() + 1);
        });
        cleanupExpiredWindows(now);
        long retryAfter = Math.max(1, windowStart + windowSeconds - now);
        return new Decision(updated.count() <= limit, retryAfter);
    }

    private void cleanupExpiredWindows(long now) {
        if (requestsUntilCleanup.decrementAndGet() > 0) {
            return;
        }
        windows.entrySet().removeIf(entry -> entry.getValue().start() + windowSeconds <= now);
        requestsUntilCleanup.set(CLEANUP_EVERY_REQUESTS);
    }

    record Decision(boolean allowed, long retryAfterSeconds) {}
    private record Window(long start, int count) {}
}
