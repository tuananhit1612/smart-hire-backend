package com.smarthire.backend.core.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Basic in-memory rate limiting for auth endpoints (login, register)
 * to prevent brute-force attacks in v1.0.0.
 */
@Slf4j
@Component
public class AuthRateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    
    // Key: IP Address, Value: [Count, ResetTimeMillis]
    private final Map<String, long[]> requestCounts = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // Only apply to /api/v1/auth paths
        if (!request.getRequestURI().startsWith("/api/v1/auth")) {
            return true;
        }

        String clientIp = getClientIp(request);
        long currentTime = System.currentTimeMillis();

        long[] stats = requestCounts.computeIfAbsent(clientIp, k -> new long[]{0, currentTime + TimeUnit.MINUTES.toMillis(1)});

        // Reset if the minute has passed
        if (currentTime > stats[1]) {
            stats[0] = 0;
            stats[1] = currentTime + TimeUnit.MINUTES.toMillis(1);
        }

        stats[0]++;

        if (stats[0] > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for IP: {} on Auth endpoints", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests. Please try again later.");
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
