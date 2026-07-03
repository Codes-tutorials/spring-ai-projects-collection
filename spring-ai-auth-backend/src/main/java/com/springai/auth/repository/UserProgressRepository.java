package com.springai.auth.repository;

import com.springai.auth.model.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for user lesson progress.
 */
@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    /**
     * Find all progress rows for a specific user + course.
     * Navigates the nested 'user.email' property.
     */
    List<UserProgress> findByUserEmailAndCourseId(String email, int courseId);

    /**
     * Find the single row for a user + course + lesson.
     */
    Optional<UserProgress> findByUserEmailAndCourseIdAndLessonId(
        String email, int courseId, int lessonId
    );

    /**
     * Count how many lessons a user has completed across ALL courses.
     */
    @Query("SELECT COUNT(p) FROM UserProgress p WHERE p.user.email = :email AND p.completed = true")
    long countAllCompletedByEmail(@Param("email") String email);

    /**
     * Count completed lessons in a specific course.
     */
    @Query("SELECT COUNT(p) FROM UserProgress p WHERE p.user.email = :email AND p.courseId = :courseId AND p.completed = true")
    long countCompletedByCourseId(@Param("email") String email, @Param("courseId") int courseId);

    /**
     * Delete all progress for a user.
     */
    void deleteByUserEmail(String email);
}
