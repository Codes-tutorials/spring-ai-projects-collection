package com.springai.auth.controller;

import com.springai.auth.model.UserProfile;
import com.springai.auth.service.JwtService;
import com.springai.auth.service.ProgressService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for per-user lesson progress.
 * All state is now persisted in the database via ProgressService.
 *
 * Handles hundreds of users correctly because:
 *  - Each row in user_progress is scoped to (email, course_id, lesson_id)
 *  - Reads and writes are transactional (Hibernate + connection pool)
 *  - Works identically across multiple backend instances (no shared JVM state)
 *  - Progress survives server restarts and deployments
 *
 * Endpoints:
 *   GET    /api/progress/{courseId}             — load completed lesson IDs
 *   POST   /api/progress/{courseId}/{lessonId}  — mark lesson complete
 *   DELETE /api/progress/{courseId}/{lessonId}  — mark lesson incomplete
 */
@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final JwtService jwtService;
    private final ProgressService progressService;

    public ProgressController(JwtService jwtService, ProgressService progressService) {
        this.jwtService = jwtService;
        this.progressService = progressService;
    }

    // ── JWT helper ────────────────────────────────────────
    private String resolveEmail(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) return null;
        UserProfile profile = jwtService.extractUserProfile(token);
        return profile.email();
    }

    // ── GET /api/progress/{courseId} ──────────────────────
    /**
     * Returns all completed lesson IDs for the authenticated user in this course.
     * Called once when the course viewer page loads.
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<?> getProgress(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable int courseId) {

        String email = resolveEmail(authHeader);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        List<Integer> completed = progressService.getCompletedLessonIds(email, courseId);
        return ResponseEntity.ok(Map.of(
                "courseId", courseId,
                "completedLessonIds", completed
        ));
    }

    // ── POST /api/progress/{courseId}/{lessonId} ──────────
    /**
     * Marks a lesson as completed and persists to DB.
     * Idempotent — calling it twice for the same lesson is safe.
     */
    @PostMapping("/{courseId}/{lessonId}")
    public ResponseEntity<?> markComplete(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable int courseId,
            @PathVariable int lessonId) {

        String email = resolveEmail(authHeader);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        List<Integer> completed = progressService.markComplete(email, courseId, lessonId);
        return ResponseEntity.ok(Map.of(
                "courseId", courseId,
                "lessonId", lessonId,
                "completed", true,
                "completedLessonIds", completed
        ));
    }

    // ── DELETE /api/progress/{courseId}/{lessonId} ────────
    /**
     * Marks a lesson as incomplete (un-check a previously completed lesson).
     */
    @DeleteMapping("/{courseId}/{lessonId}")
    public ResponseEntity<?> markIncomplete(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable int courseId,
            @PathVariable int lessonId) {

        String email = resolveEmail(authHeader);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        List<Integer> completed = progressService.markIncomplete(email, courseId, lessonId);
        return ResponseEntity.ok(Map.of(
                "courseId", courseId,
                "lessonId", lessonId,
                "completed", false,
                "completedLessonIds", completed
        ));
    }
}
