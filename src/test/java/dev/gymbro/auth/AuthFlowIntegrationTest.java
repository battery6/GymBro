package dev.gymbro.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

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

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void fullRegisterLoginRefreshLifecycle() {
        // register
        ResponseEntity<JsonNode> registered = rest.postForEntity(
                "/api/v1/auth/register",
                Map.of("email", "alice@example.com",
                        "password", "password123",
                        "displayName", "Alice",
                        "timezone", "Europe/Stockholm"),
                JsonNode.class);
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String accessToken = registered.getBody().get("accessToken").asText();
        String refreshToken = registered.getBody().get("refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // authenticated request
        ResponseEntity<JsonNode> me = rest.exchange(
                "/api/v1/users/me", HttpMethod.GET, new HttpEntity<>(bearer(accessToken)), JsonNode.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().get("email").asText()).isEqualTo("alice@example.com");
        assertThat(me.getBody().get("timezone").asText()).isEqualTo("Europe/Stockholm");

        // missing token -> 401 problem+json
        ResponseEntity<JsonNode> unauthenticated = rest.getForEntity("/api/v1/users/me", JsonNode.class);
        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unauthenticated.getBody().get("code").asText()).isEqualTo("UNAUTHENTICATED");

        // refresh rotates the token
        ResponseEntity<JsonNode> refreshed = rest.postForEntity(
                "/api/v1/auth/refresh", Map.of("refreshToken", refreshToken), JsonNode.class);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        String rotatedRefreshToken = refreshed.getBody().get("refreshToken").asText();
        assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

        // the consumed refresh token is now rejected
        ResponseEntity<JsonNode> replayed = rest.postForEntity(
                "/api/v1/auth/refresh", Map.of("refreshToken", refreshToken), JsonNode.class);
        assertThat(replayed.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // logout revokes the current refresh token
        ResponseEntity<Void> loggedOut = rest.postForEntity(
                "/api/v1/auth/logout", Map.of("refreshToken", rotatedRefreshToken), Void.class);
        assertThat(loggedOut.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ResponseEntity<JsonNode> afterLogout = rest.postForEntity(
                "/api/v1/auth/refresh", Map.of("refreshToken", rotatedRefreshToken), JsonNode.class);
        assertThat(afterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void duplicateEmailIsRejected() {
        Map<String, String> body = Map.of(
                "email", "bob@example.com", "password", "password123", "displayName", "Bob");
        assertThat(rest.postForEntity("/api/v1/auth/register", body, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        ResponseEntity<JsonNode> duplicate = rest.postForEntity("/api/v1/auth/register", body, JsonNode.class);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("code").asText()).isEqualTo("EMAIL_ALREADY_USED");
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() {
        rest.postForEntity("/api/v1/auth/register",
                Map.of("email", "carol@example.com", "password", "password123", "displayName", "Carol"),
                JsonNode.class);

        ResponseEntity<JsonNode> login = rest.postForEntity("/api/v1/auth/login",
                Map.of("email", "carol@example.com", "password", "wrong-password"), JsonNode.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(login.getBody().get("code").asText()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void registrationValidationFailsWithProblemDetails() {
        ResponseEntity<JsonNode> response = rest.postForEntity("/api/v1/auth/register",
                Map.of("email", "not-an-email", "password", "short", "displayName", ""), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().get("violations")).isNotEmpty();
    }

    @Test
    void unknownTimezoneIsRejected() {
        ResponseEntity<JsonNode> response = rest.postForEntity("/api/v1/auth/register",
                Map.of("email", "dave@example.com", "password", "password123",
                        "displayName", "Dave", "timezone", "Mars/Olympus_Mons"),
                JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code").asText()).isEqualTo("INVALID_TIMEZONE");
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
