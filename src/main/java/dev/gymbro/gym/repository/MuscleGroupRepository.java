package dev.gymbro.gym.repository;

import java.util.Optional;

import dev.gymbro.gym.entity.MuscleGroup;

import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for the migration-seeded {@link MuscleGroup} reference table (ADR-001). */
public interface MuscleGroupRepository extends JpaRepository<MuscleGroup, Long> {

    Optional<MuscleGroup> findByNameIgnoreCase(String name);
}
