# Architecture Decisions

ADR-style log of design decisions with trade-offs. Each entry is short: the
decision, why, and what was given up. Referenced from `DESIGN.md` as
`[ADR-NNN]`.

Status values: **Accepted**, **Superseded by ADR-NNN**, **Revisited**.

---

## ADR-001 — Exercise-to-muscle-group is a plain many-to-many with an `is_primary` flag

**Date:** 2026-08-31 · **Revisited:** 2026-09-01 · **Status:** Accepted (simplified)

**Context.** The reporting feature "weekly training volume trend per muscle
group" needs to attribute each set's volume to muscles. A single
`exercise.muscle_group` column can't represent compound lifts — a bench press
trains chest, triceps, and front delts — so that report would be wrong for
exactly the lifts that matter most.

**Decision (as built in `V2__gym_schema.sql`).** `muscle_group` is a **table**
(`id`, `name`); `exercise_muscle_group(exercise_id, muscle_group_id,
is_primary)` is the join, PK `(exercise_id, muscle_group_id)`, with an index on
`muscle_group_id`. A set's `reps × weight_kg` is attributed in full to each
linked muscle group; the volume report can optionally restrict to
`is_primary = true` to avoid multi-counting compound lifts.

**Original decision (2026-08-31, superseded).** `muscle_group` as an enum and a
join carrying `role` + a `contribution` weight (0–1), so a bench press could be
booked as chest 0.60 / triceps 0.25 / front-delt 0.15 and volume apportioned
fractionally.

**Why the change.** The weighted model needs a defensible contribution number
for every seeded exercise/muscle pair — subjective, time-consuming, and not
worth it for a personal tracker. A table (rather than an enum) also lets muscle
groups be managed as data without a migration.

**Trade-offs.** Volume-by-muscle can't be apportioned — a muscle is either
credited with the full set or (if filtered) not at all. Totals across muscle
groups will exceed total session volume when secondary muscles are counted.
Acceptable; revisit if the report proves misleading.

---

## ADR-002 — Instants are `timestamptz`; day grouping uses an explicit stored `date`

**Date:** 2026-08-31 · **Revisited:** 2026-09-01 · **Status:** Accepted (simplified)

**Context.** Nearly every report buckets events into days: "meals on date X",
"training days vs rest days", "trailing 7-day adherence". The day boundary has
to come from somewhere.

**Decision (as built).** Instant columns (`created_at`, `start_time`,
`end_time`, `logged_at`) are `timestamptz`. Each row that gets grouped by day
also carries an explicit `date` column — `workout_session.at_date` (default
`current_date`), and the equivalent on `meal_entry` when nutrition is built —
set from the user's local day at write time. Reports `GROUP BY` that column
directly. `app_user.timezone` is still stored for display and future use.

**Original decision (2026-08-31, superseded).** Store only `timestamptz`, no
`date` column, and derive the local day in every query as
`date(ts AT TIME ZONE app_user.timezone)`.

**Why the change.** The derived approach puts a timezone join and a function
call in the `GROUP BY` of every reporting query and can't use a plain index. A
stored `date` keeps the queries simple and indexable.

**Trade-offs.** The `date` is only as correct as whatever set it — a client
sending the wrong local day, or the server default `current_date` firing in the
server's timezone, produces a wrong bucket. The app must set `date` deliberately
from the caller's local day rather than relying on the column default. Editing a
session's timestamps does not auto-correct its `date`.

---

## ADR-003 — Sessions do not snapshot the template; sets link session + exercise directly

**Date:** 2026-08-31 · **Revisited:** 2026-09-01 · **Status:** Accepted (simplified)

**Context.** A `workout_session` may be created from a `workout_template`. If
the session reads targets from the live template, editing or deleting that
template later changes what past sessions appear to have been "planned" to be.

**Decision (as built in `V2__gym_schema.sql`).** No `session_exercise` table.
`set_entry` carries `session_id` and `exercise_id` directly. A session keeps a
nullable `template_id` (`ON DELETE SET NULL`) purely as a reference. Any
"planned vs actual" view reads `template_exercise` as it stands now.

**Original decision (2026-08-31, superseded).** Copy the template's per-exercise
targets into `session_exercise` rows at session creation so the session is a
self-contained historical record; hang `set_entry` off `session_exercise`.

**Why the change.** The snapshot adds a table and a copy step for a
planned-vs-actual comparison that isn't in the v1 feature list. What actually
happened (the `set_entry` rows) is already immutable; only the *plan* reference
is soft.

**Trade-offs.** Editing or deleting a template retroactively changes the
baseline any planned-vs-actual view would show for older sessions. If that
comparison becomes a real feature, reintroduce the snapshot (or snapshot just
the `target_*` values onto `set_entry`). The frozen-`MealEntry` approach in
ADR-004 is deliberately kept, because there the mutable input (`Food`) feeds a
*number* that is the whole point of the record.

---

## ADR-004 — `MealEntry` freezes its computed macros at log time

**Date:** 2026-08-31 · **Status:** Accepted

**Context.** Meal macros are `quantity_grams × Food.*_per_100g`. Food rows are
mutable: a user edits a custom food, or a cached USDA row is re-synced with
corrected values. Recomputing historical meals from current food data silently
rewrites the log.

**Decision.** Compute and store `calories`, `protein_g`, `carbs_g`, `fat_g`
directly on `MealEntry` at creation. Keep `food_id` for provenance and
re-logging. Reports read the frozen fields.

**Trade-offs.** Corrections to a food don't propagate to past meals (this is
the point). Four extra columns. A "recalculate from current food" action could
be offered explicitly later if wanted.

### ADR-004a — `NutritionGoal` non-overlap is enforced by a Postgres exclusion constraint

Goals are time-ranged (`effective_from`, `effective_to` nullable). Overlaps or
gaps corrupt "which goal applied on day X". Enforce with
`EXCLUDE USING gist (user_id WITH =, daterange(effective_from,
coalesce(effective_to,'infinity')) WITH &&)`. `PUT /goals` closes the current
goal and opens the new one in a single transaction. Chose a DB constraint over
application-only validation so concurrent writes can't slip through.

---

## ADR-005 — Food search is local-first with a resilient USDA fallback

**Date:** 2026-08-31 · **Status:** Accepted

**Context.** `GET /foods/search` may call USDA FoodData Central inside the
request. That's an external dependency in the hot path: latency, rate limits,
downtime, key management.

**Decision.**
- Serve local results (`Food`: user's CUSTOM + cached USDA) first, always.
- Only call USDA when local results are thin and Redis has no fresh entry.
- Hard ~800 ms timeout; wrap the client in Resilience4j (timeout + circuit
  breaker + bulkhead).
- Persist normalized USDA results into `Food` with
  `INSERT ... ON CONFLICT (source, external_id) DO UPDATE` (races are
  expected). Cache the raw USDA response in Redis keyed by normalized query,
  ~24h TTL.
- On USDA failure, return local results with `externalLookup: "unavailable"`.
  Never 5xx because USDA is down.

**Trade-offs.** Two caching layers to reason about and invalidate. USDA
normalization is real work — response shape varies by data type (Foundation /
SR Legacy / Branded), energy appears under multiple nutrient numbers — so it
gets a dedicated `UsdaFoodMapper` with fixture-based tests.

---

## ADR-006 — API is user-scoped at the repository layer, unversioned in the URL, and uses RFC 7807

**Date:** 2026-08-31 · **Revisited:** 2026-09-02 · **Status:** Accepted (versioning dropped)

- **Versioning:** all routes are under `/api` with **no version segment**.
  Originally `/api/v1`; dropped while pre-v1 (ADR-016) since there are no
  external consumers to protect and the extra segment is noise. If a
  backwards-incompatible change is ever needed after release, introduce
  `/api/v2` alongside `/api` at that point.
- **Ownership:** every user-owned resource is filtered by the authenticated
  principal in the repository/query layer. Another user's resource returns
  `404`, not `403`, so existence isn't leaked. A single enforcement point
  beats per-controller checks.
- **Errors:** RFC 7807 `application/problem+json` (native in Spring Boot 3)
  with a fixed set of `type` URIs and a `violations` array for validation
  failures.
- **Auth:** access token (short TTL) + revocable refresh token; `logout`
  revokes the refresh token. Access tokens remain non-revocable — accepted,
  mitigated by short TTL.
- **Pagination:** cursor pagination on `(started_at, id)` for time-ordered
  lists (`sessions`, `meals`); `page`/`size` elsewhere.
- **DELETE** endpoints exist for every user-created resource (mis-entered
  sets, meals, custom exercises, templates); referenced custom exercises
  return `409`.

---

## ADR-007 — `DailyLog` is computed on demand, not materialized

**Date:** 2026-08-31 · **Status:** Accepted

**Context.** Rev 1 both described computing `DailyLog` on demand *and* a
`@Scheduled` job pre-computing weekly summaries — a contradiction, and two
sources of truth.

**Decision.** Compute on demand for v1. At single-user / personal-log scale
the aggregate queries are cheap, and response caching covers repeat hits.
Defer any `@Scheduled` materialization to v1.1, and if added, frame it
explicitly as a rollup cache over the same computation — never an independent
store.

**Trade-offs.** Heavier reads if data volume grows unexpectedly. Revisit if a
report ever exceeds a comfortable latency budget.

---

## ADR-008 — Seed exercises via a Flyway migration, not an application runner

**Date:** 2026-08-31 · **Status:** Accepted

A versioned `V00X__seed_exercises.sql` (plus `ExerciseMuscle` rows) is
deterministic, runs identically in tests and prod, and is visible in migration
history. An `ApplicationRunner` that upserts on every boot is harder to test
and reason about. Trade-off: editing the seed set later needs a new migration
rather than a code change.

---

## ADR-009 — v1 is strength training only

**Date:** 2026-08-31 · **Status:** Accepted

The set model is reps × weight. Cardio, distance, and time-based work don't fit
and would dilute scope. `SetEntry` reserves nullable `duration_seconds` /
`distance_m` and `Exercise` carries a `modality` enum so the schema can extend
without a rewrite, but no v1 logic references them. Trade-off: the tracker
can't replace a running log yet.

---

## ADR-010 — No `exercise.is_custom` column; derive it from `created_by`

**Date:** 2026-08-31 · **Status:** Accepted

**Decision.** `exercise` has no `is_custom` column. "Custom" is exactly
`created_by IS NOT NULL` (null `created_by` = system-seeded). The API exposes
`isCustom` as a derived field on the DTO; queries filter on `created_by IS
[NOT] NULL`, optionally with a partial index if it ever gets hot.

**Context.** An early `V2__gym_schema.sql` draft carried a stored `is_custom`
boolean. It was removed: it duplicates information already in `created_by`, and
two columns that must agree eventually won't.

**Trade-offs.** A future notion of "custom but not owned by one user" (e.g. an
exercise shared within a group) would need a real column again — but that
feature isn't planned, and adding the column then is a trivial migration.

---

## ADR-011 — Bodyweight-exercise volume: log the effective load in `weight_kg`

**Date:** 2026-08-31 · **Revisited:** 2026-09-01 · **Status:** Accepted (simplified)

**Context.** Pull-ups and dips logged with `weight_kg = 0` contribute zero
volume, understating training load.

**Decision (as built).** No `is_bodyweight` column and no `bodyweight_kg` on
the session. The user logs the *effective* load in `weight_kg` — bodyweight
plus any added plate, or an estimate — and volume is `reps × weight_kg` like
any other set. The progression suggester treats a set logged at `weight_kg = 0`
as a signal to advance reps rather than load.

**Original decision (2026-08-31, superseded).** An `is_bodyweight` flag plus
`session.bodyweight_kg`, with `effective_weight_kg = added load + bodyweight`.

**Why the change.** It removes a column, a per-session bodyweight-capture step,
and a stale-estimate fallback, in exchange for asking the user to enter one
number they already know.

**Trade-offs.** Historical bodyweight isn't tracked automatically, so
bodyweight-only volume trends reflect whatever the user typed. A `DailyMetric`
bodyweight field (DESIGN §3.3) can feed this later if wanted.

---

## ADR-012 — The training/nutrition link is reported as descriptive stats, not "correlation"

**Date:** 2026-08-31 · **Status:** Accepted

The endpoint (`/reports/training-vs-rest`) compares mean protein intake (and
bodyweight/sleep if tracked) on days with a completed session vs days without,
and always shows the sample size. It is deliberately not labelled a
correlation: n is small, and the comparison is uncontrolled and confounded.
Trade-off: less impressive-sounding, but defensible in an interview.

---

## ADR-013 — Build a walking skeleton before breadth

**Date:** 2026-08-31 · **Status:** Accepted

The second build milestone is one complete vertical slice — register → create
session → add sets → get overload suggestion — fully tested (unit +
Testcontainers), with CI running and the image building, before any other
feature is started. One of the project's stated goals is "actually finish it";
a walking skeleton front-loads the integration risk (auth, migrations, Docker
Compose, CI) instead of discovering it late. Trade-off: the first
demo-able feature set arrives a little later.

---

## ADR-014 — Integration tests run against real Postgres (Testcontainers), never H2

**Date:** 2026-08-31 · **Status:** Accepted

The reporting queries use hand-written SQL, Postgres range types, `citext`,
date bucketing, and `ON CONFLICT`. H2's compatibility modes don't cover these,
so an H2-backed test would pass against code that fails in production.
`@DataJpaTest` and controller integration tests use a Testcontainers Postgres
matching the prod major version. Trade-off: tests need a container engine
available (Docker, or rootless podman — see README) and are a few seconds
slower to start.

---

## ADR-015 — Table names are singular; the users table is `app_user`

**Date:** 2026-09-01 · **Status:** Accepted

**Context.** Table names were standardised to singular (`exercise`,
`workout_session`, `set_entry`, …) for consistency. `user`, however, is a
reserved word in PostgreSQL — `create table user (...)` is a syntax error, and
every reference would need to be quoted `"user"`.

**Decision.** The users table is `app_user`; the refresh-token table is
`refresh_token`. Entities map with `@Table(name = "app_user")` /
`@Table(name = "refresh_token")`. The Java class stays `User`.

**Alternatives.** Keep the table plural (`users`) — the most common workaround,
but breaks the singular convention. Quote `"user"` everywhere — fragile and
ugly.

**Trade-offs.** The `app_` prefix is slightly arbitrary and appears on only
this one table. Accepted as the least-bad option; it's a well-worn convention
(Spring Security samples use it).

---

## ADR-016 — Migrations are mutable pre-v1, immutable from the first release

**Date:** 2026-09-01 · **Status:** Accepted

**"v1" here** means the first running/deployed version of the application — the
first release. It does not exist yet; the entire current codebase is pre-v1.
(Not to be confused with the Flyway file `V1__user_schema.sql`.)

**Context.** During early development the schema is still being shaped: the
`V1__…` file has been renamed, `V2__…` rewritten several times, table names
changed. Flyway's normal rule — never touch an applied migration — would make
this churn painful and would have us adding a new migration file for every
small correction.

**Decision.**
- **Pre-v1 (now, no release).** Migration files are treated as mutable: edit,
  reorder, rename, or squash them freely to keep the schema definition clean.
  Safe because no database holds a durable `flyway_schema_history` — every test
  and CI run uses a throwaway Testcontainers Postgres, and the local
  `scripts/dev-infra.sh` database has no volume. If a local dev database drifts,
  drop it and re-migrate.
- **From v1 onward (first release).** Standard Flyway immutability applies.
  Applied migrations are never edited; every schema change ships as a new
  `V{n}__*.sql`. `flyway repair` only for a known, deliberate reason. Squash
  `V1..Vn` into one clean baseline migration as part of preparing that first
  release.

**Trade-offs.** Anyone pulling the repo mid-development may need to recreate
their local database after a migration is rewritten rather than getting an
incremental upgrade. Acceptable while there are no real users and the schema is
unstable.
