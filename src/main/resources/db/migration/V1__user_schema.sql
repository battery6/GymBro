-- Case-insensitive text, used for email uniqueness.
create extension if not exists citext;

-- "user" is a reserved word in PostgreSQL, so the table is named app_user
-- (see ADR-015).
create table app_user (
    id            bigint      generated always as identity primary key,
    email         citext      not null unique,
    password_hash text        not null,
    display_name  text        not null,
    timezone      text        not null default 'UTC',
    unit_system   text        not null default 'METRIC',
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);

-- Opaque, rotating refresh tokens. Only the SHA-256 hash of the token is stored.
create table refresh_token (
    id         bigint      generated always as identity primary key,
    user_id    bigint      not null references app_user (id) on delete cascade,
    token_hash text        not null unique,
    expires_at timestamptz not null,
    created_at timestamptz not null default now(),
    revoked_at timestamptz
);

create index idx_refresh_token_user_id on refresh_token (user_id);
