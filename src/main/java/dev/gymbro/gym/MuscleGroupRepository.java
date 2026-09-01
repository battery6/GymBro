package dev.gymbro.gym;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MuscleGroupRepository extends JpaRepository<MuscleGroup, Long> {

    Optional<MuscleGroup> findByNameIgnoreCase(String name);
}
