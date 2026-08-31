-- Case-insensitive text, used for email uniqueness.
create extension if not exists citext;

create table users (
    id            bigint generated always as identity primary key,
    email         citext      not null unique,
    password_hash text        not null,
    display_name  text        not null,
    timezone      text        not null default 'UTC',
    unit_system   text        not null default 'METRIC',
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);

-- Opaque, rotating refresh tokens. Only the SHA-256 hash of the token is stored.
create table refresh_tokens (
    id         bigint generated always as identity primary key,
    user_id    bigint      not null references users (id) on delete cascade,
    token_hash text        not null unique,
    expires_at timestamptz not null,
    created_at timestamptz not null default now(),
    revoked_at timestamptz
);

create index idx_refresh_tokens_user_id on refresh_tokens (user_id);
