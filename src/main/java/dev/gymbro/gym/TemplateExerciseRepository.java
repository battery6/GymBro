package dev.gymbro.gym;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateExerciseRepository extends JpaRepository<TemplateExercise, Long> {

    List<TemplateExercise> findByTemplateIdOrderByOrderIndex(Long templateId);

    List<TemplateExercise> findByExerciseId(Long exerciseId);

    void deleteByTemplateId(Long templateId);
}
