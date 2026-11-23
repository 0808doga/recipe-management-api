package com.dogasimsek.recipeapi.service;

import com.dogasimsek.recipeapi.dto.AuthResponse;
import com.dogasimsek.recipeapi.dto.LoginRequest;
import com.dogasimsek.recipeapi.dto.RefreshTokenRequest;
import com.dogasimsek.recipeapi.dto.RegisterRequest;
import com.dogasimsek.recipeapi.dto.UserResponse;
import com.dogasimsek.recipeapi.entity.Role;
import com.dogasimsek.recipeapi.entity.User;
import com.dogasimsek.recipeapi.exception.DuplicateResourceException;
import com.dogasimsek.recipeapi.repository.UserRepository;
import com.dogasimsek.recipeapi.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, login and token refresh.
 */
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Creates a new account. New accounts always get the USER role. */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A user with email '" + email + "' already exists");
        }
        String hashedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.name().trim(), email, hashedPassword, Role.USER);
        return UserResponse.from(userRepository.save(user));
    }

    /** Checks e-mail and password, then returns new tokens. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Same message for "unknown e-mail" and "wrong password": attackers cannot guess which e-mails exist
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
        return createAuthResponse(user);
    }

    /** Gives a new access token when the refresh token is still valid. */
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String email;
        try {
            email = jwtService.extractEmailFromRefreshToken(request.refreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));
        return createAuthResponse(user);
    }

    private AuthResponse createAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                UserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
