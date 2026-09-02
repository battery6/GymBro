package dev.gymbro.gym;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import dev.gymbro.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** End-to-end tests for {@code /api/programs} and {@code /api/programs/{id}/templates}. */
class WorkoutProgramControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void createProgramThenAppendTemplatesInOrder() {
        String token = registerUser();
        long pushId = createTemplate(token, "Push A");
        long pullId = createTemplate(token, "Pull A");

        ResponseEntity<JsonNode> created = rest.exchange(
                "/api/programs", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "PPL", "description", "6-day"), bearer(token)),
                JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("name").asText()).isEqualTo("PPL");
        long programId = created.getBody().get("id").asLong();

        ResponseEntity<JsonNode> first = addTemplate(token, programId, pushId);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(first.getBody().get("orderIndex").asInt()).isZero();
        assertThat(first.getBody().get("templateName").asText()).isEqualTo("Push A");

        ResponseEntity<JsonNode> second = addTemplate(token, programId, pullId);
        assertThat(second.getBody().get("orderIndex").asInt()).isEqualTo(1);

        // Same template may appear again at a later position.
        ResponseEntity<JsonNode> third = addTemplate(token, programId, pushId);
        assertThat(third.getBody().get("orderIndex").asInt()).isEqualTo(2);

        ResponseEntity<JsonNode> detail = rest.exchange(
                "/api/programs/" + programId, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().get("templates")).hasSize(3);
    }

    @Test
    void listUpdateAndDeleteProgram() {
        String token = registerUser();
        ResponseEntity<JsonNode> created = rest.exchange(
                "/api/programs", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Bulk"), bearer(token)), JsonNode.class);
        long programId = created.getBody().get("id").asLong();

        ResponseEntity<JsonNode> list = rest.exchange(
                "/api/programs", HttpMethod.GET, new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(list.getBody()).anySatisfy(node ->
                assertThat(node.get("id").asLong()).isEqualTo(programId));

        ResponseEntity<JsonNode> updated = rest.exchange(
                "/api/programs/" + programId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", "Cut"), bearer(token)), JsonNode.class);
        assertThat(updated.getBody().get("name").asText()).isEqualTo("Cut");

        ResponseEntity<Void> deleted = rest.exchange(
                "/api/programs/" + programId, HttpMethod.DELETE,
                new HttpEntity<>(bearer(token)), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void createProgramRejectsBlankName() {
        String token = registerUser();
        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/programs", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "  "), bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void addTemplateToUnknownProgramIs404() {
        String token = registerUser();
        long templateId = createTemplate(token, "Legs");
        ResponseEntity<JsonNode> response = addTemplate(token, 9_999_999L, templateId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cannotAddAnotherUsersTemplateToOwnProgram() {
        String owner = registerUser();
        String other = registerUser();
        long ownerTemplate = createTemplate(other, "Not yours");
        long programId = createProgram(owner, "Mine");

        ResponseEntity<JsonNode> response = addTemplate(owner, programId, ownerTemplate);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cannotReadAnotherUsersProgram() {
        String owner = registerUser();
        String intruder = registerUser();
        long programId = createProgram(owner, "Private");

        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/programs/" + programId, HttpMethod.GET,
                new HttpEntity<>(bearer(intruder)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deletingTemplateReferencedByProgramIs409() {
        String token = registerUser();
        long templateId = createTemplate(token, "Slotted");
        long programId = createProgram(token, "Uses it");
        addTemplate(token, programId, templateId);

        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/templates/" + templateId, HttpMethod.DELETE,
                new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code").asText()).isEqualTo("TEMPLATE_IN_USE");
    }

    // --- helpers ---

    private String registerUser() {
        ResponseEntity<JsonNode> response = rest.postForEntity("/api/auth/register",
                Map.of("email", "prog-" + UUID.randomUUID() + "@example.com",
                        "password", "password123",
                        "displayName", "Program Tester"),
                JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().get("accessToken").asText();
    }

    private long createTemplate(String token, String name) {
        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/templates", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", name), bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().get("id").asLong();
    }

    private long createProgram(String token, String name) {
        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/programs", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", name), bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().get("id").asLong();
    }

    private ResponseEntity<JsonNode> addTemplate(String token, long programId, long templateId) {
        return rest.exchange(
                "/api/programs/" + programId + "/templates", HttpMethod.POST,
                new HttpEntity<>(Map.of("templateId", templateId), bearer(token)), JsonNode.class);
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
