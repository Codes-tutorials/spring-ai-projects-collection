package com.springai.auth.service;

import com.springai.auth.model.User;
import com.springai.auth.model.UserProgress;
import com.springai.auth.repository.UserProgressRepository;
import com.springai.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for reading and writing user lesson progress.
 * All database writes are wrapped in transactions.
 */
@Service
@Transactional
public class ProgressService {

    private final UserProgressRepository repo;
    private final UserRepository userRepository;

    public ProgressService(UserProgressRepository repo, UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    /**
     * Return the list of completed lesson IDs for a user in a specific course.
     * Called on page load to restore the sidebar check-marks.
     */
    @Transactional(readOnly = true)
    public List<Integer> getCompletedLessonIds(String email, int courseId) {
        return repo.findByUserEmailAndCourseId(email, courseId)
                   .stream()
                   .filter(UserProgress::isCompleted)
                   .map(UserProgress::getLessonId)
                   .collect(Collectors.toList());
    }

    /**
     * Mark a lesson as completed (upsert — creates the row if it does not exist).
     * Returns the full list of completed lesson IDs for the course.
     */
    public List<Integer> markComplete(String email, int courseId, int lessonId) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        UserProgress progress = repo
            .findByUserEmailAndCourseIdAndLessonId(email, courseId, lessonId)
            .orElseGet(() -> new UserProgress(user, courseId, lessonId, false));

        progress.setCompleted(true);
        repo.save(progress);

        return getCompletedLessonIds(email, courseId);
    }

    /**
     * Mark a lesson as NOT completed (un-check in the UI).
     * Returns the updated list of completed lesson IDs.
     */
    public List<Integer> markIncomplete(String email, int courseId, int lessonId) {
        repo.findByUserEmailAndCourseIdAndLessonId(email, courseId, lessonId)
            .ifPresent(p -> {
                p.setCompleted(false);
                repo.save(p);
            });

        return getCompletedLessonIds(email, courseId);
    }

    /**
     * Count how many lessons this user has completed across all courses.
     */
    @Transactional(readOnly = true)
    public long getTotalCompleted(String email) {
        return repo.countAllCompletedByEmail(email);
    }
}
