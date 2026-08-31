package dev.gymbro;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests: full application context on a random port,
 * backed by a real PostgreSQL in a container (see ADR-014). Flyway migrates the
 * container on startup.
 *
 * <p>The container is a JVM-wide singleton: started once in a static initializer
 * and never stopped (Ryuk reaps it at JVM exit). It is deliberately <em>not</em>
 * managed by {@code @Testcontainers}/{@code @Container}, which would stop it after
 * each test class while Spring keeps reusing its cached application context —
 * leaving later classes pointed at a dead container.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    static {
        POSTGRES.start();
    }
}
