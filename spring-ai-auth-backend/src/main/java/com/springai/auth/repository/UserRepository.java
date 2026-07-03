package com.springai.auth.repository;

import com.springai.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their unique email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by their unique Google sub claim.
     */
    Optional<User> findByGoogleSub(String googleSub);
}
