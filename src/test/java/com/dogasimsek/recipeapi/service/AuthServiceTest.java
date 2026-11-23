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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerSavesUserWithHashedPasswordAndUserRole() {
        when(userRepository.existsByEmail("doga@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserResponse response = authService.register(
                new RegisterRequest(" Doga ", " Doga@Example.com ", "Password123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(response.email()).isEqualTo("doga@example.com");
        assertThat(response.name()).isEqualTo("Doga");
    }

    @Test
    void registerWithExistingEmailThrowsDuplicate() {
        when(userRepository.existsByEmail("doga@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Doga", "doga@example.com", "Password123")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithCorrectPasswordReturnsTokens() {
        User user = new User("Doga", "doga@example.com", "hashed", Role.USER);
        when(userRepository.findByEmail("doga@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new LoginRequest("doga@example.com", "Password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    void loginWithWrongPasswordThrowsBadCredentials() {
        User user = new User("Doga", "doga@example.com", "hashed", Role.USER);
        when(userRepository.findByEmail("doga@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("doga@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginWithUnknownEmailThrowsBadCredentials() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "x")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshWithValidTokenReturnsNewTokens() {
        User user = new User("Doga", "doga@example.com", "hashed", Role.USER);
        when(jwtService.extractEmailFromRefreshToken("refresh")).thenReturn("doga@example.com");
        when(userRepository.findByEmail("doga@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");

        AuthResponse response = authService.refreshToken(new RefreshTokenRequest("refresh"));

        assertThat(response.accessToken()).isEqualTo("new-access");
    }

    @Test
    void refreshWithInvalidTokenThrowsBadCredentials() {
        when(jwtService.extractEmailFromRefreshToken("broken")).thenThrow(new JwtException("bad"));

        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest("broken")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid or expired refresh token");
    }
}
