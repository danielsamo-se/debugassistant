package com.debugassistant.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter filter;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setup() {
        filter = new RateLimitFilter(redisTemplate, objectMapper);
        // use 60 req/min to match the "61st request" scenario in the tests below
        ReflectionTestUtils.setField(filter, "requestsPerMinute", 60);
    }

    @SuppressWarnings("unchecked")
    private void stubRedis(long count, long ttl) {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(List.of(count, ttl));
    }

    @Test
    void shouldPassThroughGetOnAnalyzePath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/analyze");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldPassThroughPostOnOtherPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldAllowRequestAtExactLimit() throws Exception {
        stubRedis(60, 15); // 60th request — still within limit

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/analyze");
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    void shouldAllowFirstRequestInWindow() throws Exception {
        stubRedis(1, 60);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/analyze");
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void shouldReturn429OnSixtyFirstRequest() throws Exception {
        stubRedis(61, 42); // 61st request, 42s remaining

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/analyze");
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        verify(filterChain, never()).doFilter(any(), any()); // chain must NOT proceed
    }

    @Test
    void shouldReturn429ResponseBodyWithMessage() throws Exception {
        stubRedis(61, 30);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/analyze");
        request.setRemoteAddr("5.5.5.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("Rate limit exceeded");
    }

    @Test
    void shouldSetRetryAfterToTtlRemainingInWindow() throws Exception {
        stubRedis(61, 28); // 28 seconds left in the current window

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/analyze");
        request.setRemoteAddr("9.9.9.9");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader("Retry-After")).isEqualTo("28");
    }

    @Test
    void shouldSetXRateLimitRemainingToZeroOn429() throws Exception {
        stubRedis(100, 5); // well over limit

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/analyze");
        request.setRemoteAddr("8.8.8.8");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0"); // never negative
    }

    @Test
    void shouldSetXRateLimitHeadersOnAllowedRequest() throws Exception {
        stubRedis(10, 50); // 10th request, 50s left

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/analyze");
        request.setRemoteAddr("3.3.3.3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("60");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("50"); // 60 - 10
        assertThat(response.getHeader("X-RateLimit-Reset")).isNotNull();
    }

    @Test
    void shouldUseSeparateRedisKeysPerIp() throws Exception {
        stubRedis(1, 60);

        MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/api/v1/analyze");
        req1.setRemoteAddr("10.0.0.1");

        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/v1/analyze");
        req2.setRemoteAddr("10.0.0.2");

        filter.doFilter(req1, new MockHttpServletResponse(), filterChain);
        filter.doFilter(req2, new MockHttpServletResponse(), filterChain);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keyCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate, times(2)).execute(any(RedisScript.class), keyCaptor.capture(), any());

        List<List<String>> capturedKeys = keyCaptor.getAllValues();
        assertThat(capturedKeys.get(0)).containsExactly("rate_limit:10.0.0.1");
        assertThat(capturedKeys.get(1)).containsExactly("rate_limit:10.0.0.2");
    }

    @Test
    void shouldExtractRealIpFromXForwardedForHeader() throws Exception {
        stubRedis(1, 60);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/analyze");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1"); // leftmost = real client
        request.setRemoteAddr("10.0.0.1"); // proxy address

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keyCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keyCaptor.capture(), any());

        assertThat(keyCaptor.getValue()).containsExactly("rate_limit:203.0.113.5");
    }

    @Test
    void shouldPassSixtySecondWindowToRedisLuaScript() throws Exception {
        stubRedis(1, 60);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/analyze");
        request.setRemoteAddr("1.1.1.1");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(redisTemplate).execute(any(RedisScript.class), anyList(), eq("60"));
    }

    @Test
    void shouldReflectRemainingTtlInResetHeader() throws Exception {
        long ttlRemaining = 45L;
        stubRedis(5, ttlRemaining);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/analyze");
        request.setRemoteAddr("2.2.2.2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        long reset = Long.parseLong(response.getHeader("X-RateLimit-Reset"));
        long nowEpoch = System.currentTimeMillis() / 1000;
        assertThat(reset).isBetween(nowEpoch + ttlRemaining - 2, nowEpoch + ttlRemaining + 2);
    }
}
