package dev.gymbro.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.gymbro.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "gymbro.rate-limit.auth.capacity=3")
class RateLimitIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void authEndpointsAreRateLimitedPerClient() {
        Map<String, String> credentials = Map.of("email", "nobody@example.com", "password", "whatever12");

        for (int i = 0; i < 3; i++) {
            ResponseEntity<JsonNode> response =
                    rest.postForEntity("/api/auth/login", credentials, JsonNode.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<JsonNode> limited =
                rest.postForEntity("/api/auth/login", credentials, JsonNode.class);
        assertThat(limited.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(limited.getBody().get("code").asText()).isEqualTo("RATE_LIMITED");
    }
}
