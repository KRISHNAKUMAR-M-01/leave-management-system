package com.company.leavemanagement.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMinutes;
    
    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    // Generates a JWT token with the given email and role as claims. 
    // The token is signed with the secret key and has an expiration time based on the configured expiration minutes.
    public String generateToken(String email, String role) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    // Parses the JWT token and returns its claims. If the token is invalid or expired, it throws a JwtException.
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Validates the JWT token by parsing its claims. If the token is valid, it returns true; otherwise, it returns false.
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    // Extracts the email (subject) from the JWT token by parsing its claims.
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    // Extracts the role from the JWT token by parsing its claims.
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }
}
