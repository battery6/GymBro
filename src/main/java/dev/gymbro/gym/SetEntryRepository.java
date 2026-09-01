package dev.gymbro.gym;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SetEntryRepository extends JpaRepository<SetEntry, Long> {

    List<SetEntry> findBySessionIdOrderBySetIndex(Long sessionId);

    List<SetEntry> findBySessionIdAndExerciseIdOrderBySetIndex(Long sessionId, Long exerciseId);

    void deleteBySessionId(Long sessionId);
}
