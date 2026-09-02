package dev.gymbro.gym;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import dev.gymbro.AbstractIntegrationTest;
import dev.gymbro.gym.entity.Exercise;
import dev.gymbro.gym.repository.ExerciseRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** End-to-end tests for {@code /api/templates} and {@code /api/templates/{id}/exercises}. */
class WorkoutTemplateControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ExerciseRepository exerciseRepository;

    @Test
    void createTemplateThenAppendExercisesInOrder() {
        String token = registerUser();
        Long benchId = createExercise("Bench Press");
        Long squatId = createExercise("Back Squat");

        ResponseEntity<JsonNode> created = rest.exchange(
                "/api/templates", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Push A", "description", "chest / shoulders"), bearer(token)),
                JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("name").asText()).isEqualTo("Push A");
        assertThat(created.getBody().get("description").asText()).isEqualTo("chest / shoulders");
        long templateId = created.getBody().get("id").asLong();

        ResponseEntity<JsonNode> first = addExercise(token, templateId, benchId, 3, 8, 8.0);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(first.getBody().get("orderIndex").asInt()).isZero();
        assertThat(first.getBody().get("exerciseId").asLong()).isEqualTo(benchId);
        assertThat(first.getBody().get("exerciseName").asText()).isEqualTo("Bench Press");
        assertThat(first.getBody().get("targetRpe").asDouble()).isEqualTo(8.0);

        ResponseEntity<JsonNode> second = addExercise(token, templateId, squatId, 5, 5, null);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getBody().get("orderIndex").asInt()).isEqualTo(1);
        assertThat(second.getBody().get("exerciseName").asText()).isEqualTo("Back Squat");
        assertThat(second.getBody().get("targetRpe").isNull()).isTrue();
    }

    @Test
    void listGetUpdateAndDeleteTemplate() {
        String token = registerUser();
        Long benchId = createExercise("Overhead Press");
        long templateId = createTemplate(token, "Upper A");
        addExercise(token, templateId, benchId, 3, 8, 8.0);

        ResponseEntity<JsonNode> list = rest.exchange(
                "/api/templates", HttpMethod.GET, new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).anySatisfy(node ->
                assertThat(node.get("id").asLong()).isEqualTo(templateId));

        ResponseEntity<JsonNode> detail = rest.exchange(
                "/api/templates/" + templateId, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().get("exercises")).hasSize(1);
        assertThat(detail.getBody().get("exercises").get(0).get("exerciseName").asText())
                .isEqualTo("Overhead Press");

        ResponseEntity<JsonNode> updated = rest.exchange(
                "/api/templates/" + templateId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", "Upper A (revised)"), bearer(token)), JsonNode.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().get("name").asText()).isEqualTo("Upper A (revised)");

        ResponseEntity<Void> deleted = rest.exchange(
                "/api/templates/" + templateId, HttpMethod.DELETE,
                new HttpEntity<>(bearer(token)), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<JsonNode> gone = rest.exchange(
                "/api/templates/" + templateId, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(gone.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cannotGetAnotherUsersTemplate() {
        String owner = registerUser();
        String intruder = registerUser();
        long templateId = createTemplate(owner, "Private");

        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/templates/" + templateId, HttpMethod.GET,
                new HttpEntity<>(bearer(intruder)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createTemplateRequiresAuthentication() {
        ResponseEntity<JsonNode> response =
                rest.postForEntity("/api/templates", Map.of("name", "X"), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code").asText()).isEqualTo("UNAUTHENTICATED");
    }

    @Test
    void createTemplateRejectsBlankName() {
        String token = registerUser();
        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/templates", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "  "), bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void addExerciseRejectsNonPositiveTargets() {
        String token = registerUser();
        long templateId = createTemplate(token, "Legs");
        Long exerciseId = createExercise("Leg Press");

        ResponseEntity<JsonNode> response = addExerciseRaw(token, templateId, Map.of(
                "exerciseId", exerciseId, "targetSets", 0, "targetReps", 8, "targetRpe", 8.0));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void addExerciseRejectsOutOfRangeRpe() {
        String token = registerUser();
        long templateId = createTemplate(token, "Pull");
        Long exerciseId = createExercise("Pulldown");

        ResponseEntity<JsonNode> response = addExercise(token, templateId, exerciseId, 3, 10, 11.0);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void addExerciseToUnknownTemplateIs404() {
        String token = registerUser();
        Long exerciseId = createExercise("Row");

        ResponseEntity<JsonNode> response = addExercise(token, 9_999_999L, exerciseId, 3, 8, 8.0);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code").asText()).isEqualTo("NOT_FOUND");
    }

    @Test
    void addUnknownExerciseIs404() {
        String token = registerUser();
        long templateId = createTemplate(token, "Full body");

        ResponseEntity<JsonNode> response = addExercise(token, templateId, 9_999_999L, 3, 8, 8.0);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cannotAddExerciseToAnotherUsersTemplate() {
        String owner = registerUser();
        String intruder = registerUser();
        long templateId = createTemplate(owner, "Owner's template");
        Long exerciseId = createExercise("Deadlift");

        ResponseEntity<JsonNode> response = addExercise(intruder, templateId, exerciseId, 3, 5, 8.0);
        // 404, not 403 — resource existence is not leaked to non-owners (ADR-006).
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- helpers ---

    private String registerUser() {
        ResponseEntity<JsonNode> response = rest.postForEntity("/api/auth/register",
                Map.of("email", "tmpl-" + UUID.randomUUID() + "@example.com",
                        "password", "password123",
                        "displayName", "Template Tester"),
                JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().get("accessToken").asText();
    }

    private Long createExercise(String name) {
        Exercise exercise = new Exercise();
        exercise.setName(name);
        return exerciseRepository.save(exercise).getId();
    }

    private long createTemplate(String token, String name) {
        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/templates", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", name), bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().get("id").asLong();
    }

    private ResponseEntity<JsonNode> addExercise(
            String token, long templateId, Long exerciseId, int sets, int reps, Double rpe) {
        Map<String, Object> body = new HashMap<>();
        body.put("exerciseId", exerciseId);
        body.put("targetSets", sets);
        body.put("targetReps", reps);
        body.put("targetRpe", rpe);
        return addExerciseRaw(token, templateId, body);
    }

    private ResponseEntity<JsonNode> addExerciseRaw(String token, long templateId, Map<String, ?> body) {
        return rest.exchange(
                "/api/templates/" + templateId + "/exercises", HttpMethod.POST,
                new HttpEntity<>(body, bearer(token)), JsonNode.class);
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
