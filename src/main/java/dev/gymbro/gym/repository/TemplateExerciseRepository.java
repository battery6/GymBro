package dev.gymbro.gym.repository;

import java.util.List;

import dev.gymbro.gym.entity.TemplateExercise;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateExerciseRepository extends JpaRepository<TemplateExercise, Long> {

    List<TemplateExercise> findByTemplateIdOrderByOrderIndex(Long templateId);

    List<TemplateExercise> findByExerciseId(Long exerciseId);

    void deleteByTemplateId(Long templateId);
}
