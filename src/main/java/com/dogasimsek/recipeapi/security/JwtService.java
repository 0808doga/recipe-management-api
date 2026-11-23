package com.dogasimsek.recipeapi.security;

import com.dogasimsek.recipeapi.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Creates and checks JSON Web Tokens (JWT).
 * There are two token types:
 * - access token: sent with every request (short lifetime)
 * - refresh token: only used to get a new access token (longer lifetime)
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS_TYPE = "access";
    private static final String REFRESH_TYPE = "refresh";

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
                      @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        if (secret == null || secret.isBlank()) {
            // No JWT_SECRET set: create a random key, so no secret is ever stored in the code.
            // Tokens then stop working after a restart (just log in again).
            log.warn("JWT_SECRET is not set. Using a random key: tokens become invalid after a restart.");
            this.signingKey = Jwts.SIG.HS256.key().build();
        } else if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            // HMAC-SHA256 needs a key of at least 256 bits (32 bytes)
            throw new IllegalStateException("JWT secret must be at least 32 characters long. Set JWT_SECRET.");
        } else {
            this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim(TYPE_CLAIM, ACCESS_TYPE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(TYPE_CLAIM, REFRESH_TYPE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    /** Returns the e-mail inside a valid access token, or throws JwtException. */
    public String extractEmailFromAccessToken(String token) {
        return extractEmail(token, ACCESS_TYPE);
    }

    /** Returns the e-mail inside a valid refresh token, or throws JwtException. */
    public String extractEmailFromRefreshToken(String token) {
        return extractEmail(token, REFRESH_TYPE);
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }

    private String extractEmail(String token, String expectedType) {
        // parseSignedClaims checks the signature AND the expiration date
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!expectedType.equals(claims.get(TYPE_CLAIM, String.class))) {
            throw new JwtException("Wrong token type");
        }
        return claims.getSubject();
    }
}
