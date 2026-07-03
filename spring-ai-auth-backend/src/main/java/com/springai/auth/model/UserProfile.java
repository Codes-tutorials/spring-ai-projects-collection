package com.springai.auth.model;

/**
 * Immutable DTO representing a Google-authenticated user with payment status.
 */
public record UserProfile(
    String sub,
    String email,
    String name,
    String picture,
    boolean emailVerified,
    boolean paid
) {}
