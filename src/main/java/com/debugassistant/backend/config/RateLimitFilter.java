package com.debugassistant.backend.config;

import com.debugassistant.backend.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Rate limiter for analyze endpoint with 60 requests per minute per IP
 */
@Component
@ConditionalOnProperty(name = "rate.limit.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_PATH = "/api/v1/analyze";
    private static final String KEY_PREFIX = "rate_limit:";
    private static final int WINDOW_SECONDS = 60;

    // Redis rate limit script
    private static final DefaultRedisScript<List> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return {current, redis.call('TTL', KEYS[1])}
            """, List.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rate.limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Rate limit only POST /api/v1/analyze
        return !(request.getMethod().equals("POST")
                && request.getRequestURI().equals(RATE_LIMIT_PATH));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String ip = extractClientIp(request);
        String key = KEY_PREFIX + ip;

        List<Long> result = (List<Long>) redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(key),
                String.valueOf(WINDOW_SECONDS)
        );

        long count = result.get(0);
        long ttl = result.get(1);

        response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, requestsPerMinute - count)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(Instant.now().getEpochSecond() + ttl));

        if (count > requestsPerMinute) {
            log.warn("Rate limit exceeded for IP {}: {} requests in window", ip, count);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(ttl));
            response.setHeader("X-RateLimit-Remaining", "0");

            ErrorResponse body = new ErrorResponse(
                    "Rate limit exceeded. Try again in " + ttl + " seconds.",
                    Instant.now(),
                    request.getRequestURI()
            );
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        // Client IP from proxy or fallback
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
