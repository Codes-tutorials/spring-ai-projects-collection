package com.springai.auth.model;

/**
 * Represents a single lesson completion record for a user.
 */
public record ProgressRecord(
    String email,
    int courseId,
    int lessonId,
    boolean completed
) {}
