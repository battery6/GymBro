package dev.gymbro.gym.repository;

import java.util.List;

import dev.gymbro.gym.entity.SetEntry;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SetEntryRepository extends JpaRepository<SetEntry, Long> {

    List<SetEntry> findBySessionIdOrderBySetIndex(Long sessionId);

    List<SetEntry> findBySessionIdAndExerciseIdOrderBySetIndex(Long sessionId, Long exerciseId);

    boolean existsByExerciseId(Long exerciseId);

    void deleteBySessionId(Long sessionId);
}
