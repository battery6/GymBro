package dev.gymbro.gym.repository;

import java.util.List;

import dev.gymbro.gym.entity.ProgramTemplate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramTemplateRepository extends JpaRepository<ProgramTemplate, Long> {

    List<ProgramTemplate> findByProgramIdOrderByOrderIndex(Long programId);

    List<ProgramTemplate> findByTemplateId(Long templateId);

    void deleteByProgramId(Long programId);
}
