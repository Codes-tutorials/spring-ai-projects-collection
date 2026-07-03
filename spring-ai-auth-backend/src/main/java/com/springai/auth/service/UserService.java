package com.springai.auth.service;

import com.springai.auth.model.User;
import com.springai.auth.model.UserProfile;
import com.springai.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Service providing transactional business logic for user profiles and authentication.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Looks up user by email. If not found, registers a new user.
     * Updates profile details from Google OAuth profile data and records last login.
     */
    public User registerOrUpdateUser(UserProfile profile) {
        User user = userRepository.findByEmail(profile.email())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(profile.email());
                    newUser.setCreatedAt(Instant.now());
                    newUser.setPaid(false);
                    return newUser;
                });

        user.setGoogleSub(profile.sub());
        user.setName(profile.name());
        user.setPicture(profile.picture());
        user.setEmailVerified(profile.emailVerified());
        user.setLastLoginAt(Instant.now());

        return userRepository.save(user);
    }

    /**
     * Finds a user by their unique email.
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Marks the user's status as paid in the database.
     */
    public User markUserAsPaid(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        user.setPaid(true);
        return userRepository.save(user);
    }
}
