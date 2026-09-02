package dev.gymbro.user.repository;

import java.util.Optional;

import dev.gymbro.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link User}. Lookup and existence checks are by email, which
 * is the login identifier and unique.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
