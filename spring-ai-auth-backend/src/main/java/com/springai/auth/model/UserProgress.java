package com.springai.auth.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Persists a single lesson completion record for a user in the database.
 *
 * Table: user_progress
 * Columns: id, user_id, course_id, lesson_id, completed, updated_at
 *
 * Unique constraint on (user_id, course_id, lesson_id) ensures one row per
 * user + course + lesson combination — safe to UPSERT via JPA save().
 */
@Entity
@Table(
    name = "user_progress",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_course_lesson",
        columnNames = { "user_id", "course_id", "lesson_id" }
    )
)
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_progress_user"))
    private User user;

    /** The course this progress belongs to (e.g. 1 = Spring AI Getting Started) */
    @Column(name = "course_id", nullable = false)
    private int courseId;

    /** The lesson within the course */
    @Column(name = "lesson_id", nullable = false)
    private int lessonId;

    /** true = completed, false = not yet completed */
    @Column(nullable = false)
    private boolean completed = false;

    /** Timestamp of the last state change — useful for analytics */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // ── Constructors ───────────────────────────────────────
    public UserProgress() {}

    public UserProgress(User user, int courseId, int lessonId, boolean completed) {
        this.user = user;
        this.courseId = courseId;
        this.lessonId = lessonId;
        this.completed = completed;
        this.updatedAt = Instant.now();
    }

    // ── Getters & Setters ──────────────────────────────────
    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public int getLessonId() { return lessonId; }
    public void setLessonId(int lessonId) { this.lessonId = lessonId; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) {
        this.completed = completed;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() { return updatedAt; }
}
