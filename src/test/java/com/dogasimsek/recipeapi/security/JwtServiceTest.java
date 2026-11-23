package com.dogasimsek.recipeapi.security;

import com.dogasimsek.recipeapi.entity.Role;
import com.dogasimsek.recipeapi.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-long-enough-123456";

    private final JwtService jwtService = new JwtService(SECRET, 60_000, 120_000);
    private final User user = new User("Doga", "doga@example.com", "hash", Role.USER);

    @Test
    void accessTokenContainsEmail() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractEmailFromAccessToken(token)).isEqualTo("doga@example.com");
        assertThat(jwtService.getAccessTokenExpirationSeconds()).isEqualTo(60);
    }

    @Test
    void refreshTokenContainsEmail() {
        String token = jwtService.generateRefreshToken(user);

        assertThat(jwtService.extractEmailFromRefreshToken(token)).isEqualTo("doga@example.com");
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        String refreshToken = jwtService.generateRefreshToken(user);

        assertThatThrownBy(() -> jwtService.extractEmailFromAccessToken(refreshToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithOtherKeyIsRejected() {
        JwtService otherService = new JwtService("another-secret-key-that-is-also-long-enough-999", 60_000, 60_000);
        String foreignToken = otherService.generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.extractEmailFromAccessToken(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService shortLived = new JwtService(SECRET, -1_000, -1_000);
        String expiredToken = shortLived.generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.extractEmailFromAccessToken(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void missingSecretUsesRandomKey() {
        JwtService first = new JwtService("", 60_000, 60_000);
        JwtService second = new JwtService(null, 60_000, 60_000);
        String token = first.generateAccessToken(user);

        assertThat(first.extractEmailFromAccessToken(token)).isEqualTo("doga@example.com");
        // a different random key cannot read the token (like after a restart)
        assertThatThrownBy(() -> second.extractEmailFromAccessToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecretIsNotAllowed() {
        assertThatThrownBy(() -> new JwtService("too-short", 1000, 1000))
                .isInstanceOf(IllegalStateException.class);
    }
}
