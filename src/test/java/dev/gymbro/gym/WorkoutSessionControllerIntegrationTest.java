package dev.gymbro.gym;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
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

/** End-to-end tests for {@code /api/sessions} and its nested {@code /sets}. */
class WorkoutSessionControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ExerciseRepository exerciseRepository;

    @Test
    void createFreeformSessionLogSetsAndReadBack() {
        String token = registerUser();
        Long benchId = seedExercise("Bench Press");

        ResponseEntity<JsonNode> created = rest.exchange(
                "/api/sessions", HttpMethod.POST,
                new HttpEntity<>(Map.of("atDate", "2026-09-02", "notes", "evening"), bearer(token)),
                JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("templateId").isNull()).isTrue();
        assertThat(created.getBody().get("complete").asBoolean()).isFalse();
        long sessionId = created.getBody().get("id").asLong();

        ResponseEntity<JsonNode> sets = rest.exchange(
                "/api/sessions/" + sessionId + "/sets", HttpMethod.POST,
                new HttpEntity<>(List.of(
                        Map.of("exerciseId", benchId, "reps", 8, "weightKg", 60.0, "rpe", 7.5),
                        Map.of("exerciseId", benchId, "reps", 8, "weightKg", 60.0)),
                        bearer(token)),
                JsonNode.class);
        assertThat(sets.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(sets.getBody()).hasSize(2);
        assertThat(sets.getBody().get(0).get("setIndex").asInt()).isZero();
        assertThat(sets.getBody().get(1).get("setIndex").asInt()).isEqualTo(1);
        assertThat(sets.getBody().get(1).get("warmup").asBoolean()).isFalse();

        ResponseEntity<JsonNode> detail = rest.exchange(
                "/api/sessions/" + sessionId, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().get("sets")).hasSize(2);
    }

    @Test
    void patchEndTimeMarksSessionComplete() {
        String token = registerUser();
        long sessionId = createSession(token);

        ResponseEntity<JsonNode> patched = rest.exchange(
                "/api/sessions/" + sessionId, HttpMethod.PATCH,
                new HttpEntity<>(Map.of("endTime", Instant.now().toString()), bearer(token)),
                JsonNode.class);

        assertThat(patched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patched.getBody().get("complete").asBoolean()).isTrue();
        assertThat(patched.getBody().get("endTime").isNull()).isFalse();
    }

    @Test
    void createFromUnknownTemplateIs404() {
        String token = registerUser();
        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/sessions", HttpMethod.POST,
                new HttpEntity<>(Map.of("templateId", 9_999_999L), bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void logSetRejectsNonPositiveReps() {
        String token = registerUser();
        long sessionId = createSession(token);
        Long exerciseId = seedExercise("Row");

        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/sessions/" + sessionId + "/sets", HttpMethod.POST,
                new HttpEntity<>(List.of(
                        Map.of("exerciseId", exerciseId, "reps", 0, "weightKg", 40.0)), bearer(token)),
                JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void logSetAcceptsZeroWeight() {
        String token = registerUser();
        long sessionId = createSession(token);
        Long exerciseId = seedExercise("Pull-up");

        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/sessions/" + sessionId + "/sets", HttpMethod.POST,
                new HttpEntity<>(List.of(
                        Map.of("exerciseId", exerciseId, "reps", 10, "weightKg", 0)), bearer(token)),
                JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get(0).get("weightKg").asDouble()).isZero();
    }

    @Test
    void deleteSetRemovesItFromTheSession() {
        String token = registerUser();
        long sessionId = createSession(token);
        Long exerciseId = seedExercise("Curl");
        ResponseEntity<JsonNode> logged = rest.exchange(
                "/api/sessions/" + sessionId + "/sets", HttpMethod.POST,
                new HttpEntity<>(List.of(
                        Map.of("exerciseId", exerciseId, "reps", 12, "weightKg", 15.0)), bearer(token)),
                JsonNode.class);
        long setId = logged.getBody().get(0).get("id").asLong();

        ResponseEntity<Void> deleted = rest.exchange(
                "/api/sessions/" + sessionId + "/sets/" + setId, HttpMethod.DELETE,
                new HttpEntity<>(bearer(token)), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<JsonNode> detail = rest.exchange(
                "/api/sessions/" + sessionId, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(detail.getBody().get("sets")).isEmpty();
    }

    @Test
    void cannotReadAnotherUsersSession() {
        String owner = registerUser();
        String intruder = registerUser();
        long sessionId = createSession(owner);

        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/sessions/" + sessionId, HttpMethod.GET,
                new HttpEntity<>(bearer(intruder)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listFiltersByDateRange() {
        String token = registerUser();
        createSessionOn(token, "2026-01-10");
        createSessionOn(token, "2026-06-15");

        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/sessions?from=2026-06-01&to=2026-06-30", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).get("atDate").asText()).isEqualTo("2026-06-15");
    }

    // --- helpers ---

    private String registerUser() {
        ResponseEntity<JsonNode> response = rest.postForEntity("/api/auth/register",
                Map.of("email", "sess-" + UUID.randomUUID() + "@example.com",
                        "password", "password123",
                        "displayName", "Session Tester"),
                JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().get("accessToken").asText();
    }

    private Long seedExercise(String name) {
        Exercise exercise = new Exercise();
        exercise.setName(name);
        return exerciseRepository.save(exercise).getId();
    }

    private long createSession(String token) {
        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/sessions", HttpMethod.POST,
                new HttpEntity<>(Map.of(), bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().get("id").asLong();
    }

    private void createSessionOn(String token, String date) {
        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/sessions", HttpMethod.POST,
                new HttpEntity<>(Map.of("atDate", date), bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
