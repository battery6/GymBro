package dev.gymbro.gym.repository;

import java.util.List;
import java.util.Optional;

import dev.gymbro.gym.entity.TemplateExercise;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateExerciseRepository extends JpaRepository<TemplateExercise, Long> {

    List<TemplateExercise> findByTemplateIdOrderByOrderIndex(Long templateId);

    List<TemplateExercise> findByExerciseId(Long exerciseId);

    boolean existsByExerciseId(Long exerciseId);

    Optional<TemplateExercise> findFirstByTemplateIdOrderByOrderIndexDesc(Long templateId);

    void deleteByTemplateId(Long templateId);
}
