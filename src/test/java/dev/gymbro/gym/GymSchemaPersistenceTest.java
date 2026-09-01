package dev.gymbro.gym;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import dev.gymbro.AbstractIntegrationTest;
import dev.gymbro.gym.entity.Exercise;
import dev.gymbro.gym.entity.ExerciseMuscleGroup;
import dev.gymbro.gym.entity.MuscleGroup;
import dev.gymbro.gym.entity.ProgramTemplate;
import dev.gymbro.gym.entity.SetEntry;
import dev.gymbro.gym.entity.TemplateExercise;
import dev.gymbro.gym.entity.WorkoutProgram;
import dev.gymbro.gym.entity.WorkoutSession;
import dev.gymbro.gym.entity.WorkoutTemplate;
import dev.gymbro.gym.repository.ExerciseMuscleGroupRepository;
import dev.gymbro.gym.repository.ExerciseRepository;
import dev.gymbro.gym.repository.MuscleGroupRepository;
import dev.gymbro.gym.repository.ProgramTemplateRepository;
import dev.gymbro.gym.repository.SetEntryRepository;
import dev.gymbro.gym.repository.TemplateExerciseRepository;
import dev.gymbro.gym.repository.WorkoutProgramRepository;
import dev.gymbro.gym.repository.WorkoutSessionRepository;
import dev.gymbro.gym.repository.WorkoutTemplateRepository;
import dev.gymbro.user.entity.User;
import dev.gymbro.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises every {@code V2__gym_schema.sql} table through its entity and
 * repository. With {@code ddl-auto: none} this is what catches a column-name or
 * type mismatch between the entities and the migration.
 */
@Transactional
class GymSchemaPersistenceTest extends AbstractIntegrationTest {

    @Autowired UserRepository users;
    @Autowired ExerciseRepository exercises;
    @Autowired MuscleGroupRepository muscleGroups;
    @Autowired ExerciseMuscleGroupRepository exerciseMuscleGroups;
    @Autowired WorkoutTemplateRepository templates;
    @Autowired WorkoutProgramRepository programs;
    @Autowired ProgramTemplateRepository programTemplates;
    @Autowired WorkoutSessionRepository sessions;
    @Autowired TemplateExerciseRepository templateExercises;
    @Autowired SetEntryRepository setEntries;

    @Test
    void persistsTheWholeGymGraph() {
        User user = users.saveAndFlush(newUser());

        Exercise bench = new Exercise();
        bench.setName("Bench Press");
        bench.setEquipment("barbell");
        bench = exercises.saveAndFlush(bench);
        assertThat(bench.getId()).isNotNull();
        assertThat(bench.isCustom()).isFalse();
        assertThat(bench.getCreatedAt()).isNotNull();

        Exercise custom = new Exercise();
        custom.setName("Cable Y-Raise");
        custom.setCreatedBy(user.getId());
        custom = exercises.saveAndFlush(custom);
        assertThat(custom.isCustom()).isTrue();
        assertThat(exercises.findByCreatedByIsNull()).extracting(Exercise::getId).contains(bench.getId());
        assertThat(exercises.findByIdAndCreatedBy(custom.getId(), user.getId())).isPresent();

        MuscleGroup chest = new MuscleGroup();
        chest.setName("chest");
        chest = muscleGroups.saveAndFlush(chest);
        assertThat(muscleGroups.findByNameIgnoreCase("CHEST")).isPresent();

        exerciseMuscleGroups.saveAndFlush(new ExerciseMuscleGroup(bench.getId(), chest.getId(), true));
        assertThat(exerciseMuscleGroups.findByMuscleGroupId(chest.getId()))
                .singleElement()
                .satisfies(link -> assertThat(link.isPrimary()).isTrue());

        WorkoutTemplate template = new WorkoutTemplate();
        template.setUserId(user.getId());
        template.setName("Push A");
        template = templates.saveAndFlush(template);
        assertThat(templates.findByIdAndUserId(template.getId(), user.getId())).isPresent();

        TemplateExercise planned = new TemplateExercise();
        planned.setTemplateId(template.getId());
        planned.setExerciseId(bench.getId());
        planned.setOrderIndex(0);
        planned.setTargetSets(3);
        planned.setTargetReps(8);
        planned.setTargetRpe(new BigDecimal("8.0"));
        templateExercises.saveAndFlush(planned);
        assertThat(templateExercises.findByTemplateIdOrderByOrderIndex(template.getId())).hasSize(1);

        WorkoutProgram program = new WorkoutProgram();
        program.setUserId(user.getId());
        program.setName("PPL");
        program = programs.saveAndFlush(program);

        ProgramTemplate slot = new ProgramTemplate();
        slot.setProgramId(program.getId());
        slot.setTemplateId(template.getId());
        slot.setOrderIndex(0);
        programTemplates.saveAndFlush(slot);
        assertThat(programTemplates.findByProgramIdOrderByOrderIndex(program.getId())).hasSize(1);

        WorkoutSession session = new WorkoutSession();
        session.setUserId(user.getId());
        session.setTemplateId(template.getId());
        session.setAtDate(LocalDate.of(2026, 9, 1));
        session.setStartTime(Instant.now());
        session = sessions.saveAndFlush(session);
        assertThat(session.isComplete()).isFalse();

        SetEntry warmup = new SetEntry();
        warmup.setSessionId(session.getId());
        warmup.setExerciseId(bench.getId());
        warmup.setSetIndex(0);
        warmup.setReps(10);
        warmup.setWeightKg(new BigDecimal("20.00"));
        warmup.setWarmup(true);
        setEntries.saveAndFlush(warmup);

        SetEntry working = new SetEntry();
        working.setSessionId(session.getId());
        working.setExerciseId(bench.getId());
        working.setSetIndex(1);
        working.setReps(8);
        working.setWeightKg(new BigDecimal("60.00"));
        working.setRpe(new BigDecimal("7.5"));
        setEntries.saveAndFlush(working);

        assertThat(setEntries.findBySessionIdOrderBySetIndex(session.getId())).hasSize(2);
        assertThat(setEntries.findBySessionIdAndExerciseIdOrderBySetIndex(session.getId(), bench.getId()))
                .extracting(SetEntry::getRpe)
                .containsExactly(null, new BigDecimal("7.5"));
        assertThat(sessions.findByUserIdAndAtDateBetweenOrderByAtDate(
                user.getId(), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1))).hasSize(1);
    }

    private static User newUser() {
        User u = new User();
        u.setEmail("gym-" + UUID.randomUUID() + "@example.com");
        u.setPasswordHash("x");
        u.setDisplayName("Gym Tester");
        return u;
    }
}
