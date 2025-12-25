package com.debugassistant.backend.controller;

import com.debugassistant.backend.dto.auth.AuthResponse;
import com.debugassistant.backend.dto.auth.LoginRequest;
import com.debugassistant.backend.dto.auth.RegisterRequest;
import com.debugassistant.backend.exception.EmailAlreadyRegisteredException;
import com.debugassistant.backend.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthenticationService authenticationService;

    @Test
    void shouldRegisterUser() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Test User");
        AuthResponse response = new AuthResponse("jwt_token", 86400000L, "test@example.com", "Test User");

        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt_token"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(authenticationService, times(1)).register(any(RegisterRequest.class));
        verifyNoMoreInteractions(authenticationService);
    }

    @Test
    void shouldLoginUser() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        AuthResponse response = new AuthResponse("jwt_token", 86400000L, "test@example.com", "Test User");

        when(authenticationService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt_token"));

        verify(authenticationService, times(1)).login(any(LoginRequest.class));
        verifyNoMoreInteractions(authenticationService);
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("invalid-email", "password123", "Test");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid email format")));

        verifyNoInteractions(authenticationService);
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyRegistered() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Test");

        when(authenticationService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyRegisteredException("Email already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Email already registered")));

        verify(authenticationService, times(1)).register(any(RegisterRequest.class));
        verifyNoMoreInteractions(authenticationService);
    }

    @Test
    void shouldReturnUnauthorizedOnBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "wrong");

        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Invalid email or password")));

        verify(authenticationService, times(1)).login(any(LoginRequest.class));
        verifyNoMoreInteractions(authenticationService);
    }

    @Test
    void shouldReturnBadRequestForMissingBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid JSON payload")));

        verifyNoInteractions(authenticationService);
    }
}