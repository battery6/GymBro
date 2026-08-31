# GymBro

Backend platform for tracking workouts and nutrition, with logic that connects
the two (progressive-overload suggestions, macro adherence, training-vs-rest
comparisons).

- **Design:** [`DESIGN.md`](DESIGN.md)
- **Decision log:** [`DECISIONS.md`](DECISIONS.md)

## Stack

Java 21 · Spring Boot 3.3 · PostgreSQL · Flyway · Spring Data JPA · Spring
Security + JWT · Redis · Testcontainers · springdoc-openapi · Docker Compose ·
GitHub Actions.

## Prerequisites

- JDK 21 (the build targets 21; newer JDKs may print warnings)
- Docker + Docker Compose (for Postgres/Redis and integration tests)
- Maven is **optional** — `./mvnw` downloads a pinned Maven if `mvn` is absent

## Quick start

```bash
# 1. start Postgres + Redis
docker compose up -d db redis

# 2. run the app (Flyway migrates on startup)
./mvnw spring-boot:run

# 3. or run everything in containers
cp .env.example .env          # set a real JWT_SECRET
docker compose up --build
```

App: <http://localhost:8080> · Swagger UI: <http://localhost:8080/swagger-ui.html>
· Health: <http://localhost:8080/actuator/health>

## Auth flow (implemented in step 1)

```
POST /api/v1/auth/register   { email, password, displayName, timezone? } -> 201 { accessToken, refreshToken, ... }
POST /api/v1/auth/login      { email, password }                         -> 200 { accessToken, refreshToken, ... }
POST /api/v1/auth/refresh    { refreshToken }                            -> 200 { new pair }  (old refresh token is revoked)
POST /api/v1/auth/logout     { refreshToken }                            -> 204
GET  /api/v1/users/me        Authorization: Bearer <accessToken>         -> 200 { id, email, displayName, timezone, unitSystem }
```

Access tokens are stateless HS256 JWTs (15 min). Refresh tokens are opaque,
stored SHA-256-hashed, rotated on use, and revocable. Auth endpoints are
IP-rate-limited. Errors are RFC 7807 `application/problem+json`.

## Tests

```bash
./mvnw verify
```

Integration tests spin up a real PostgreSQL via Testcontainers (no H2 — see
[ADR-014](DECISIONS.md)). Docker must be running.

## Build order

See [`DESIGN.md` §5](DESIGN.md). Step 1 (foundation + auth) is done; step 2 is
the walking skeleton (exercise → session → sets → suggestion).
