-- Tables are created in dependency order so each FK target already exists.

create table exercise (
    id bigint generated always as identity primary key,
    created_by bigint references app_user(id) on delete set null,
    name text not null,
    equipment text,
    description text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table muscle_group (
    id bigint generated always as identity primary key,
    name text not null unique,
    created_at timestamptz not null default now()
);

create table exercise_muscle_group (
    exercise_id bigint not null references exercise(id) on delete cascade,
    muscle_group_id bigint not null references muscle_group(id) on delete cascade,
    is_primary boolean not null default false,
    primary key (exercise_id, muscle_group_id)
);

create table workout_template (
    id bigint generated always as identity primary key,
    user_id bigint not null references app_user(id),
    name text not null,
    description text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table workout_program (
    id bigint generated always as identity primary key,
    user_id bigint not null references app_user(id),
    name text not null,
    description text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table program_template (
    id bigint generated always as identity primary key,
    program_id bigint not null references workout_program(id) on delete cascade,
    template_id bigint not null references workout_template(id),
    order_index integer not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (program_id, order_index)
);

create table workout_session (
    id bigint generated always as identity primary key,
    user_id bigint not null references app_user(id),
    template_id bigint references workout_template(id) on delete set null,
    at_date date not null default current_date,
    start_time timestamptz not null default now(),
    end_time timestamptz,
    notes text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table template_exercise (
    id bigint generated always as identity primary key,
    template_id bigint not null references workout_template(id) on delete cascade,
    exercise_id bigint not null references exercise(id),
    order_index integer not null,
    target_sets integer not null,
    target_reps integer not null,
    target_rpe numeric(3, 1),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (template_id, order_index)
);

create table set_entry (
    id bigint generated always as identity primary key,
    session_id bigint not null references workout_session(id) on delete cascade,
    exercise_id bigint not null references exercise(id),
    set_index integer not null,
    reps integer not null,
    weight_kg numeric(6, 2) not null,
    rpe numeric(3, 1),
    is_warmup boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- Foreign key columns aren't indexed automatically in Postgres (only the
-- primary key is). Add indexes on FKs that will be filtered/joined on
-- frequently -- user_id lookups and session/template lookups especially.
create index idx_workout_template_user_id on workout_template(user_id);
create index idx_workout_program_user_id on workout_program(user_id);
create index idx_program_template_program_id on program_template(program_id);
create index idx_program_template_template_id on program_template(template_id);
create index idx_template_exercise_template_id on template_exercise(template_id);
create index idx_workout_session_user_id on workout_session(user_id);
create index idx_workout_session_template_id on workout_session(template_id);
create index idx_set_entry_session_id on set_entry(session_id);
create index idx_exercise_muscle_group_muscle_group_id on exercise_muscle_group(muscle_group_id);
