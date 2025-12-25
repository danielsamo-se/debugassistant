package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.auth.AuthResponse;
import com.debugassistant.backend.dto.auth.LoginRequest;
import com.debugassistant.backend.dto.auth.RegisterRequest;
import com.debugassistant.backend.entity.User;
import com.debugassistant.backend.exception.EmailAlreadyRegisteredException;
import com.debugassistant.backend.exception.UserNotFoundException;
import com.debugassistant.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthenticationService authenticationService;

    @Test
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Test User");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt_token");
        when(jwtService.getExpirationTime()).thenReturn(86400000L);

        AuthResponse response = authenticationService.register(request);

        assertThat(response.token()).isEqualTo("jwt_token");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.name()).isEqualTo("Test User");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded_password");
    }

    @Test
    void shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123", "Test");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginUser() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .name("Test User")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt_token");
        when(jwtService.getExpirationTime()).thenReturn(86400000L);

        AuthResponse response = authenticationService.login(request);

        assertThat(response.token()).isEqualTo("jwt_token");
        assertThat(response.email()).isEqualTo("test@example.com");

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("test@example.com", "password123")
        );
    }

    @Test
    void shouldRejectLoginForNonexistentUser() {
        LoginRequest request = new LoginRequest("unknown@example.com", "password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }
}