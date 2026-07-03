package com.springai.auth.service;

import com.springai.auth.model.UserProfile;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for generating and validating the application's own JWTs.
 * These JWTs are issued to the Angular frontend after successful Google OAuth2 authentication.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed JWT embedding the user's profile claims.
     */
    public String generateToken(UserProfile user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.sub());
        claims.put("email", user.email());
        claims.put("name", user.name());
        claims.put("picture", user.picture());
        claims.put("emailVerified", user.emailVerified());
        claims.put("paid", user.paid());

        return Jwts.builder()
                .claims(claims)
                .subject(user.email())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Parses and validates a JWT, returning its claims.
     */
    public Claims validateAndExtractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks whether a JWT is structurally valid and not expired.
     */
    public boolean isTokenValid(String token) {
        try {
            validateAndExtractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts a {@link UserProfile} from the JWT claims.
     */
    public UserProfile extractUserProfile(String token) {
        Claims claims = validateAndExtractClaims(token);
        return new UserProfile(
                claims.get("sub", String.class),
                claims.get("email", String.class),
                claims.get("name", String.class),
                claims.get("picture", String.class),
                Boolean.TRUE.equals(claims.get("emailVerified", Boolean.class)),
                Boolean.TRUE.equals(claims.get("paid", Boolean.class))
        );
    }
}
