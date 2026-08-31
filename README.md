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

- JDK 21 (the build targets 21). `./mvnw` auto-detects a JDK at
  `~/.local/jdk-21`, `/usr/lib/jvm/java-21-openjdk`, or Android Studio's
  bundled JDK; otherwise set `JAVA_HOME`.
- Maven is **optional** — `./mvnw` downloads a pinned Maven if `mvn` is absent
- A container engine for Postgres/Redis and the Testcontainers integration
  tests: **Docker**, or **rootless podman** (see below)

## Quick start

```bash
# 1. start Postgres + Redis
docker compose up -d db redis        # Docker
scripts/dev-infra.sh up              # rootless podman

# 2. run the app (Flyway migrates on startup)
./mvnw spring-boot:run

# 3. or run everything in containers (Docker only)
cp .env.example .env                 # set a real JWT_SECRET
docker compose up --build
```

### Rootless podman setup (Fedora)

One-time, no root required:

```bash
# 1. Docker-compatible API socket
systemctl --user enable --now podman.socket

# 2. let Testcontainers find it and skip the Ryuk reaper (needs privileges
#    rootless podman doesn't grant)
cat > ~/.testcontainers.properties <<'EOF'
docker.host=unix:///run/user/1000/podman/podman.sock
ryuk.disabled=true
EOF

# 3. resolve unqualified image names (e.g. postgres:16-alpine) without a
#    registry prompt
mkdir -p ~/.config/containers
cat > ~/.config/containers/registries.conf <<'EOF'
unqualified-search-registries = ["docker.io", "registry.fedoraproject.org", "registry.access.redhat.com"]
short-name-mode = "permissive"
EOF
```

Then `./mvnw verify` and `scripts/dev-infra.sh up` work. Replace `1000` with
your `id -u` if different.

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
[ADR-014](DECISIONS.md)), so a container engine must be reachable — Docker, or
rootless podman configured as above.

## Build order

See [`DESIGN.md` §5](DESIGN.md). Step 1 (foundation + auth) is done; step 2 is
the walking skeleton (exercise → session → sets → suggestion).
