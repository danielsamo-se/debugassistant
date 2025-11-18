package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.auth.AuthResponse;
import com.debugassistant.backend.dto.auth.LoginRequest;
import com.debugassistant.backend.dto.auth.RegisterRequest;
import com.debugassistant.backend.entity.User;
import com.debugassistant.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for user authentication: register and login
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.email());

        // check if email is already registered
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        // generate JWT for the new user
        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                jwtService.getExpirationTime(),
                user.getEmail(),
                user.getName()
        );
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.email());

        // authenticate user credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // generate JWT for the user
        String token = jwtService.generateToken(user);
        log.info("User logged in successfully: {}", user.getEmail());

        return new AuthResponse(
                token,
                jwtService.getExpirationTime(),
                user.getEmail(),
                user.getName()
        );
    }
}