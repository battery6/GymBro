package dev.gymbro.gym.repository;

import java.util.List;
import java.util.Optional;

import dev.gymbro.gym.entity.ProgramTemplate;

import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for the ordered template slots that make up a {@link dev.gymbro.gym.entity.WorkoutProgram}. */
public interface ProgramTemplateRepository extends JpaRepository<ProgramTemplate, Long> {

    List<ProgramTemplate> findByProgramIdOrderByOrderIndex(Long programId);

    List<ProgramTemplate> findByTemplateId(Long templateId);

    /** The current last slot, so a new one can be appended at {@code orderIndex + 1}. */
    Optional<ProgramTemplate> findFirstByProgramIdOrderByOrderIndexDesc(Long programId);

    /** Whether any program still references this template — blocks its deletion. */
    boolean existsByTemplateId(Long templateId);

    void deleteByProgramId(Long programId);
}
