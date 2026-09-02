package dev.gymbro.gym;

import static org.assertj.core.api.Assertions.assertThat;

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

/** End-to-end tests for {@code /api/exercises}. */
class ExerciseControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ExerciseRepository exerciseRepository;

    @Test
    void listReturnsLibraryPlusOwnCustomButNotOthers() {
        Long libraryId = seedLibraryExercise("Barbell Bench Press");
        String alice = registerUser();
        String bob = registerUser();

        Long aliceCustom = createExercise(alice, "Alice's Cable Fly");
        createExercise(bob, "Bob's Landmine Press");

        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/exercises", HttpMethod.GET, new HttpEntity<>(bearer(alice)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().findValuesAsText("id")).isNotNull();
        assertThat(response.getBody()).anySatisfy(node -> {
            assertThat(node.get("id").asLong()).isEqualTo(libraryId);
            assertThat(node.get("isCustom").asBoolean()).isFalse();
        });
        assertThat(response.getBody()).anySatisfy(node -> {
            assertThat(node.get("id").asLong()).isEqualTo(aliceCustom);
            assertThat(node.get("isCustom").asBoolean()).isTrue();
        });
        assertThat(response.getBody()).noneSatisfy(node ->
                assertThat(node.get("name").asText()).isEqualTo("Bob's Landmine Press"));
    }

    @Test
    void createRejectsBlankName() {
        String token = registerUser();
        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/exercises", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "  "), bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void deleteOwnUnreferencedCustomExercise() {
        String token = registerUser();
        Long exerciseId = createExercise(token, "Temp Exercise");

        ResponseEntity<Void> response = rest.exchange(
                "/api/exercises/" + exerciseId, HttpMethod.DELETE,
                new HttpEntity<>(bearer(token)), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(exerciseRepository.findById(exerciseId)).isEmpty();
    }

    @Test
    void cannotDeleteLibraryExercise() {
        String token = registerUser();
        Long libraryId = seedLibraryExercise("Immutable Library Lift");

        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/exercises/" + libraryId, HttpMethod.DELETE,
                new HttpEntity<>(bearer(token)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exerciseRepository.findById(libraryId)).isPresent();
    }

    @Test
    void cannotDeleteAnotherUsersCustomExercise() {
        String owner = registerUser();
        String intruder = registerUser();
        Long exerciseId = createExercise(owner, "Owner's Move");

        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/exercises/" + exerciseId, HttpMethod.DELETE,
                new HttpEntity<>(bearer(intruder)), JsonNode.class);

        // 404, not 403 — existence isn't leaked to non-owners (ADR-006).
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteReferencedCustomExerciseIs409() {
        String token = registerUser();
        Long exerciseId = createExercise(token, "Referenced Move");
        long templateId = createTemplate(token, "Template using it");
        addExerciseToTemplate(token, templateId, exerciseId);

        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/exercises/" + exerciseId, HttpMethod.DELETE,
                new HttpEntity<>(bearer(token)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code").asText()).isEqualTo("EXERCISE_IN_USE");
    }

    // --- helpers ---

    private String registerUser() {
        ResponseEntity<JsonNode> response = rest.postForEntity("/api/auth/register",
                Map.of("email", "ex-" + UUID.randomUUID() + "@example.com",
                        "password", "password123",
                        "displayName", "Exercise Tester"),
                JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().get("accessToken").asText();
    }

    private Long seedLibraryExercise(String name) {
        Exercise exercise = new Exercise();
        exercise.setName(name);
        return exerciseRepository.save(exercise).getId();
    }

    private Long createExercise(String token, String name) {
        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/exercises", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", name), bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().get("id").asLong();
    }

    private long createTemplate(String token, String name) {
        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/templates", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", name), bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().get("id").asLong();
    }

    private void addExerciseToTemplate(String token, long templateId, Long exerciseId) {
        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/templates/" + templateId + "/exercises", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "exerciseId", exerciseId, "targetSets", 3, "targetReps", 8), bearer(token)),
                JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
