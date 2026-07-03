package com.springai.auth.model;

/**
 * Response payload returned to the Angular frontend after successful authentication or payment.
 * Contains the signed JWT and the user's profile.
 */
public record AuthResponse(
    String token,
    UserProfile user
) {}
