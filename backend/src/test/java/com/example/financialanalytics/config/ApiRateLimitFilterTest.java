package com.example.financialanalytics.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiRateLimitFilterTest {
    @Test
    void rejectsExcessRequestsWithRetryAfterAndDoesNotCountCorsPreflight() throws Exception {
        var service = new RateLimitService(new FixedClock(Instant.parse("2026-10-14T12:00:05Z")),
                60, 1, 1);
        var filter = new ApiRateLimitFilter(service, new ObjectMapper());
        var passes = new AtomicInteger();
        var request = request("/api/v1/account", "GET");

        filter.doFilter(request, new MockHttpServletResponse(),
                (servletRequest, servletResponse) -> passes.incrementAndGet());

        MockHttpServletResponse limited = new MockHttpServletResponse();
        filter.doFilter(request, limited, (servletRequest, servletResponse) -> passes.incrementAndGet());
        assertEquals(1, passes.get());
        assertEquals(429, limited.getStatus());
        assertEquals("55", limited.getHeader("Retry-After"));
        assertEquals("{\"message\":\"Too many requests. Please retry later.\"}",
                limited.getContentAsString());

        var preflight = request("/api/v1/account", "OPTIONS");
        filter.doFilter(preflight, new MockHttpServletResponse(),
                (servletRequest, servletResponse) -> passes.incrementAndGet());
        assertEquals(2, passes.get());
    }

    @Test
    void appliesStricterLimitToAuthenticationEndpoints() throws Exception {
        var service = new RateLimitService(Clock.fixed(
                Instant.parse("2026-10-14T12:00:05Z"), ZoneOffset.UTC), 60, 5, 1);
        var filter = new ApiRateLimitFilter(service, new ObjectMapper());
        var login = request("/api/v1/auth/login", "POST");
        var first = new MockHttpServletResponse();
        var second = new MockHttpServletResponse();

        filter.doFilter(login, first, (servletRequest, servletResponse) -> {});
        filter.doFilter(login, second, (servletRequest, servletResponse) -> {});

        assertEquals(200, first.getStatus());
        assertEquals(429, second.getStatus());
    }

    private MockHttpServletRequest request(String path, String method) {
        var request = new MockHttpServletRequest();
        request.setRequestURI(path);
        request.setServletPath(path);
        request.setMethod(method);
        request.setRemoteAddr("192.0.2.4");
        return request;
    }

    private static final class FixedClock extends Clock {
        private final AtomicReference<Instant> instant;

        private FixedClock(Instant instant) {
            this.instant = new AtomicReference<>(instant);
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
