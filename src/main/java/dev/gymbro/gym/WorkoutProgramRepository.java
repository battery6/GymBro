package dev.gymbro.gym;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutProgramRepository extends JpaRepository<WorkoutProgram, Long> {

    List<WorkoutProgram> findByUserId(Long userId);

    Optional<WorkoutProgram> findByIdAndUserId(Long id, Long userId);
}
