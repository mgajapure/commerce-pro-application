package com.commerce_pro_backend.user_identity.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtTokenProvider {

    private final String jwtSecret;
    private final long jwtExpirationMs;
    @Getter
    private final long refreshExpirationMs;
    
    private SecretKey key;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration-ms:900000}") long jwtExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.jwtSecret = jwtSecret;
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                "app.jwt.secret must be configured. Set JWT_SECRET environment variable.");
        }
        // Reject the default placeholder — forces developers to set a real secret
        if (jwtSecret.startsWith("your-256-bit-secret-key")) {
            throw new IllegalStateException(
                "JWT secret is using the default placeholder value. " +
                "Set a secure JWT_SECRET environment variable before starting the application.");
        }
        byte[] keyBytes = jwtSecret.getBytes();
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT secret must be at least 32 characters long for HS256 (got " + keyBytes.length + ").");
        }
        key = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT token provider initialized with key length: {} bytes", keyBytes.length);
    }

    public String generateToken(Authentication authentication, boolean isSuperAdmin) {
        String username = authentication.getName();
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtExpirationMs, ChronoUnit.MILLIS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        claims.put("type", "ACCESS");
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("isSuperAdmin", isSuperAdmin);

        return Jwts.builder()
            .claims(claims)
            .subject(username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(key, Jwts.SIG.HS512)
            .compact();
    }

    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshExpirationMs, ChronoUnit.MILLIS);

        return Jwts.builder()
            .subject(username)
            .claim("type", "REFRESH")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(key, Jwts.SIG.HS512)
            .compact();
    }

    public String generateImpersonationToken(String adminUsername, String targetUserId, String targetUsername) {
        Instant now = Instant.now();
        Instant expiration = now.plus(30, ChronoUnit.MINUTES);

        return Jwts.builder()
            .subject(targetUsername)
            .claim("type", "IMPERSONATION")
            .claim("originalAdmin", adminUsername)
            .claim("targetUserId", targetUserId)
            .claim("isImpersonating", true)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(key, Jwts.SIG.HS512)
            .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("JWT token is malformed: {}", e.getMessage());
        } catch (SecurityException e) {
            log.error("JWT signature validation failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty or null: {}", e.getMessage());
        }
        return false;
    }

    public boolean isImpersonationToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "IMPERSONATION".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "REFRESH".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public Date getExpirationDateFromToken(String token) {
        return parseToken(token).getExpiration();
    }

    public String getTokenId(String token) {
        Claims claims = parseToken(token);
        return claims.get("jti", String.class);
    }

    /**
     * Returns SHA-256 hex hash of the raw token string.
     * Used to store/look up refresh tokens without persisting the raw value.
     */
    public String hashToken(String rawToken) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

}