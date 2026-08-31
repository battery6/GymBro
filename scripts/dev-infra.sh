#!/usr/bin/env bash
# Start / stop local Postgres + Redis for running the app outside containers
# (e.g. `./mvnw spring-boot:run`). Uses rootless podman. Credentials and ports
# match docker-compose.yml and the application.yml defaults.
#
#   scripts/dev-infra.sh up      # start (idempotent)
#   scripts/dev-infra.sh down    # stop and remove
#   scripts/dev-infra.sh status
set -euo pipefail

POD=gymbro-dev
PG_IMAGE=docker.io/library/postgres:16-alpine
REDIS_IMAGE=docker.io/library/redis:7-alpine

case "${1:-up}" in
  up)
    if ! podman pod exists "$POD"; then
      podman pod create --name "$POD" -p 5432:5432 -p 6379:6379
    fi
    podman container exists gymbro-db || podman run -d --pod "$POD" --name gymbro-db \
      -e POSTGRES_DB=gymbro -e POSTGRES_USER=gymbro -e POSTGRES_PASSWORD=gymbro \
      "$PG_IMAGE"
    podman container exists gymbro-redis || podman run -d --pod "$POD" --name gymbro-redis \
      "$REDIS_IMAGE"
    echo "up: Postgres localhost:5432, Redis localhost:6379 (pod '$POD')"
    ;;
  down)
    podman pod rm -f "$POD" >/dev/null 2>&1 || true
    echo "down"
    ;;
  status)
    podman pod ps --filter name="$POD"
    podman ps --filter pod="$POD" --format '  {{.Names}}\t{{.Image}}\t{{.Status}}'
    ;;
  *)
    echo "usage: $0 [up|down|status]" >&2
    exit 1
    ;;
esac
