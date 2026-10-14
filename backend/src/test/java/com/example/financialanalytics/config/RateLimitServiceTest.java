package com.example.financialanalytics.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {
    @Test
    void appliesIndependentRequestLimitsAndResetsAtWindowBoundary() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-14T12:00:05Z"));
        RateLimitService service = new RateLimitService(clock, 60, 2, 1);

        assertTrue(service.acquire("api:192.0.2.1", false).allowed());
        assertTrue(service.acquire("api:192.0.2.1", false).allowed());
        var blocked = service.acquire("api:192.0.2.1", false);
        assertFalse(blocked.allowed());
        assertEquals(55, blocked.retryAfterSeconds());
        assertTrue(service.acquire("api:192.0.2.2", false).allowed());
        assertTrue(service.acquire("auth:192.0.2.1", true).allowed());
        assertFalse(service.acquire("auth:192.0.2.1", true).allowed());

        clock.advanceSeconds(55);
        assertTrue(service.acquire("api:192.0.2.1", false).allowed());
    }

    @Test
    void rejectsNonPositiveConfiguration() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new RateLimitService(Clock.systemUTC(), 0, 10, 5));
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private MutableClock(Instant initial) {
            instant = new AtomicReference<>(initial);
        }

        void advanceSeconds(long seconds) {
            instant.updateAndGet(current -> current.plusSeconds(seconds));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
