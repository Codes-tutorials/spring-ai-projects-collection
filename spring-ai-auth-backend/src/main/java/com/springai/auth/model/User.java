package com.springai.auth.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Persists user details captured from Google OAuth2 profile.
 * Maps to the 'users' table.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_google_sub", columnList = "google_sub")
    }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "google_sub", unique = true, length = 255)
    private String googleSub;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 2048)
    private String picture;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false)
    private boolean paid = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_login_at", nullable = false)
    private Instant lastLoginAt = Instant.now();

    // ── Constructors ───────────────────────────────────────
    public User() {}

    public User(String email, String googleSub, String name, String picture, boolean emailVerified) {
        this.email = email;
        this.googleSub = googleSub;
        this.name = name;
        this.picture = picture;
        this.emailVerified = emailVerified;
        this.paid = false;
        this.createdAt = Instant.now();
        this.lastLoginAt = Instant.now();
    }

    // ── Getters & Setters ──────────────────────────────────
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
