package com.debugassistant.backend.service;

import com.debugassistant.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
    }

    @Test
    void shouldGenerateToken() {
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hash")
                .build();

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void shouldExtractUsername() {
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hash")
                .build();

        String token = jwtService.generateToken(user);
        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    void shouldValidateToken() {
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hash")
                .build();

        String token = jwtService.generateToken(user);
        boolean isValid = jwtService.isTokenValid(token, user);

        assertThat(isValid).isTrue();
    }

    @Test
    void shouldRejectTokenForDifferentUser() {
        User user1 = User.builder()
                .email("user1@example.com")
                .passwordHash("hash")
                .build();

        User user2 = User.builder()
                .email("user2@example.com")
                .passwordHash("hash")
                .build();

        String token = jwtService.generateToken(user1);
        boolean isValid = jwtService.isTokenValid(token, user2);

        assertThat(isValid).isFalse();
    }

    @Test
    void shouldReturnExpirationTime() {
        long expiration = jwtService.getExpirationTime();

        assertThat(expiration).isEqualTo(86400000L);
    }
}