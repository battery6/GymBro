# Gym & Nutrition Tracker — Design Doc

> Revision 2 (2026-08-31). Revision 1 was the initial draft; this revision folds
> in a design review. Decisions with trade-offs are recorded in `DECISIONS.md`
> and cross-referenced below as `[ADR-NNN]`.

## 1. Overview

A backend platform for tracking workouts and nutrition, with logic that connects
the two (e.g. correlating training performance with nutrition/recovery). Built
as a portfolio project to demonstrate backend engineering skills for job
applications, and used daily as a personal training/nutrition log.

**Primary goals**
- Replace spreadsheet-based workout and food logging with a proper app
- Demonstrate real backend engineering: relational data modeling, business
  logic beyond CRUD, testing, containerization, CI
- Stay scoped enough to actually finish and iterate on

**Out of scope (v1)**
- Frontend (API-first; a UI can come later if desired)
- Multi-tenant/team features
- Mobile app / push notifications
- Barcode scanning for food (nice-to-have, not v1)
- Cardio / time-based / distance-based training. v1 is **strength training
  only** — the set model is reps × weight. The schema leaves room to extend
  (see 3.1) but no v1 logic depends on it. `[ADR-009]`

---

## 2. Tech stack

| Concern              | Choice                                  |
|-----------------------|------------------------------------------|
| Language / framework  | Java 21, Spring Boot 3                  |
| Database               | PostgreSQL                              |
| Migrations             | Flyway                                  |
| ORM                     | Spring Data JPA / Hibernate            |
| Auth                    | Spring Security + JWT (access + refresh) |
| Caching                 | Redis (external food-lookup cache)     |
| External API            | USDA FoodData Central                  |
| Testing                 | JUnit 5, Mockito, Testcontainers (real Postgres, no H2) |
| API docs                | springdoc-openapi (Swagger UI)         |
| Error format            | RFC 7807 Problem Details (native in Spring Boot 3) |
| Build                   | Maven                                  |
| Containerization        | Docker + Docker Compose                |
| CI                      | GitHub Actions                         |
| Observability           | Spring Boot Actuator, structured (JSON) logging, Micrometer |

---

## 3. Domain model

### Conventions

- Instants (`created_at`, `start_time`, `logged_at`, …) are stored as
  `timestamptz`. Calendar-day grouping uses an explicit stored `date` on the
  logged row (`workout_session.at_date`, `meal_entry` date) rather than deriving
  it per-user-timezone. `[ADR-002]`
- All user-owned resources are scoped to the authenticated principal at the
  repository layer. Requests for another user's resource return `404`, not
  `403`, so resource existence is not leaked. `[ADR-006]`
- Entities are never serialized directly; controllers map to/from `record`
  DTOs.
- Canonical units: mass in kilograms, food quantities in grams, energy in
  kilocalories. Display-unit conversion (lb/kg) is a client concern driven by
  `User.unit_system`.

### 3.1 Gym module

> As built in `V1__user_schema.sql` and `V2__gym_schema.sql`. Table names are
> singular (`app_user`, not `user`, because `user` is a reserved word — see
> ADR-015).

**app_user**
- `id`, `email` (unique, citext), `password_hash` (BCrypt), `display_name`,
  `timezone` (IANA, e.g. `Europe/Stockholm`), `unit_system` (METRIC | IMPERIAL),
  `created_at`, `updated_at`

**exercise**
- `id`, `created_by` (nullable FK to `app_user`, `ON DELETE SET NULL`; null =
  system-seeded), `name`, `equipment` (text, nullable), `description`
  (nullable), `created_at`, `updated_at`
- No `is_custom` column — "custom" is exactly `created_by IS NOT NULL`, exposed
  as a derived `isCustom` field on the DTO. `[ADR-010]`
- Seeded via a Flyway migration (`V00X__seed_exercises.sql`), not an
  application startup runner — deterministic, versioned, testable. `[ADR-008]`

**muscle_group** (a table, not an enum)
- `id`, `name` (unique), `created_at`
- Also seeded via migration. `[ADR-001]`

**exercise_muscle_group** (join — an exercise trains several muscles)
- `exercise_id` (FK `exercise`, cascade), `muscle_group_id` (FK `muscle_group`,
  cascade), `is_primary` (bool, default false)
- PK `(exercise_id, muscle_group_id)`; index on `muscle_group_id` for the
  volume-by-muscle report.
- Volume attribution is by membership, optionally filtered to `is_primary` —
  there is no fractional contribution weight in v1. `[ADR-001]`

**workout_template**
- `id`, `user_id` (FK `app_user`), `name`, `description` (nullable),
  `created_at`, `updated_at`
- A reusable *plan* for one session, e.g. "Push Day A".

**workout_program**
- `id`, `user_id` (FK `app_user`), `name`, `description` (nullable),
  `created_at`, `updated_at`
- A multi-day plan (e.g. "PPL 6-day") that sequences templates.

**program_template**
- `id`, `program_id` (FK `workout_program`, cascade), `template_id`
  (FK `workout_template`), `order_index`, `created_at`, `updated_at`
- `UNIQUE (program_id, order_index)`. One template may appear in several
  programs (and more than once in a program, at different positions).

**template_exercise**
- `id`, `template_id` (FK `workout_template`, cascade), `exercise_id`
  (FK `exercise`), `order_index`, `target_sets`, `target_reps` (single int),
  `target_rpe` (nullable, `numeric(3,1)`), `created_at`, `updated_at`
- `UNIQUE (template_id, order_index)`.

**workout_session**
- `id`, `user_id` (FK `app_user`), `template_id` (nullable FK,
  `ON DELETE SET NULL` — sessions can be freeform), `at_date` (date, default
  `current_date`), `start_time` (timestamptz), `end_time` (nullable
  timestamptz — a session is "complete" once this is set), `notes`,
  `created_at`, `updated_at`
- `at_date` is stored explicitly rather than derived from a timezone-adjusted
  timestamp. `[ADR-002]`

**set_entry**
- `id`, `session_id` (FK `workout_session`, cascade), `exercise_id`
  (FK `exercise`), `set_index`, `reps`, `weight_kg` (`numeric(6,2)`),
  `rpe` (nullable, `numeric(3,1)`), `is_warmup` (bool, default false),
  `created_at`, `updated_at`
- Sets hang off the session and name their exercise directly; there is no
  `session_exercise` snapshot table. `[ADR-003]`

**Volume**
- Working-set volume for a set = `reps × weight_kg`, warmups excluded.
- Bodyweight movements are logged with the effective load in `weight_kg`
  (v1 has no dedicated bodyweight handling). `[ADR-011]`

**Design notes**
- Templates and Sessions are still separate concepts — a `workout_template` is
  a plan, a `workout_session` is what happened. Because sessions do not
  snapshot the template, a "planned vs actual" comparison reads the template
  as it stands *now*; editing a template changes that reference for past
  sessions. Accepted for v1. `[ADR-003]`
- `template_exercise.order_index` allows reordering without relying on
  insertion order.
- Supersets/circuits are not modeled in v1 (`order_index` can't express
  grouping). Known limitation.

### 3.2 Nutrition module

**Food**
- `id`, `name`, `brand` (nullable), `source` (ENUM: CUSTOM, USDA),
  `external_id` (nullable — USDA `fdcId`),
  `calories_per_100g`, `protein_per_100g`, `carbs_per_100g`, `fat_per_100g`,
  `created_by` (nullable FK to User), `synced_at` (nullable — when the USDA
  row was last normalized)
- Unique constraint on `(source, external_id)`. USDA results are persisted
  with an upsert (`INSERT ... ON CONFLICT (source, external_id) DO UPDATE`)
  so concurrent searches can't create duplicates. `[ADR-005]`
- USDA normalization is non-trivial: response shape differs across data types
  (Foundation, SR Legacy, Branded) and energy is reported under multiple
  nutrient numbers. Normalization lives in a dedicated
  `UsdaFoodMapper` with its own unit tests over recorded fixtures.

**MealEntry**
- `id`, `user_id`, `food_id`, `quantity_grams`,
  `meal_type` (ENUM: BREAKFAST, LUNCH, DINNER, SNACK), `logged_at`,
  `calories`, `protein_g`, `carbs_g`, `fat_g`
- The four macro/energy fields are **computed and frozen at log time** from
  `quantity_grams × Food.*_per_100g`. Later corrections to the underlying
  `Food` (especially a re-synced USDA row) do not rewrite history. The
  `food_id` is kept for provenance and re-logging convenience. `[ADR-004]`
- Stores an explicit `date` alongside `logged_at`, set from the client's local
  day (consistent with `workout_session.at_date`, `[ADR-002]`).

**NutritionGoal**
- `id`, `user_id`, `calories`, `protein_g`, `carbs_g`, `fat_g`,
  `effective_from` (date), `effective_to` (date, nullable — null = current)
- Non-overlap is enforced in the database:
  `EXCLUDE USING gist (user_id WITH =, daterange(effective_from,
  coalesce(effective_to, 'infinity')) WITH &&)`.
- `PUT /goals` runs in one transaction: close the current goal
  (`effective_to = today - 1` ... or `today`, see ADR) and insert the new one.
  `[ADR-004a]` (range handling)
- Macro/calorie internal consistency (`4p + 4c + 9f ≈ calories`) is validated
  with a tolerance and returns `400` on gross mismatch.

**Design notes**
- Two-layer caching for food search: (1) normalized results live permanently
  in `Food` (`source = USDA`); (2) Redis caches the raw USDA response keyed by
  normalized query string with a short TTL (~24h) to absorb repeat searches
  before they reach Postgres or the external API.

### 3.3 Connective / reporting layer

**DailyLog** — computed on demand, not stored. `[ADR-007]`
- Aggregates per user per day (`workout_session.at_date` / `meal_entry` date):
  total working-set volume, volume by muscle group, total calories, macro
  totals, the active `NutritionGoal` for that day, and whether a session was
  completed (`end_time IS NOT NULL`).
- A `@Scheduled` weekly-summary materialization is explicitly deferred to
  v1.1. If added, it is framed as a cache/rollup layer over the same
  computation, not a second source of truth.

**DailyMetric** (optional stretch entity)
- `id`, `user_id`, `date`, `bodyweight_kg` (nullable), `sleep_hours`
  (nullable)
- Feeds the training-vs-rest comparison.

**Reporting queries to build** (the non-CRUD logic):

1. **Weekly training volume trend per muscle group** — hand-written SQL.
   Joins `set_entry` → `exercise` → `exercise_muscle_group` → `muscle_group`,
   attributes each set's `reps × weight_kg` to every linked muscle group
   (optionally `is_primary` only), buckets by ISO week of
   `workout_session.at_date`. Watch for N+1; use a projection query.

2. **Progressive-overload suggestion** — pure function, no repository access.
   Spec'd in section 6.

3. **Macro adherence over trailing 7 / 30 days** — per day: consumed macros
   vs the goal effective that day; report mean absolute % deviation and
   days-on-target. Uses frozen `MealEntry` macros.

4. **Training-vs-rest comparison** (`/reports/training-vs-rest`, renamed from
   "correlation") — mean protein intake (and bodyweight/sleep if tracked) on
   days with a completed session vs days without. Presented as descriptive
   statistics with the sample size shown; deliberately **not** called a
   correlation, because n is small and confounded. `[ADR-012]`

---

## 4. API surface

All routes are under `/api/v1`. `[ADR-006]`

```
POST   /api/v1/auth/register
POST   /api/v1/auth/login                -> { accessToken, refreshToken }
POST   /api/v1/auth/refresh              -> { accessToken, refreshToken }
POST   /api/v1/auth/logout               (revokes the refresh token)

GET    /api/v1/exercises
POST   /api/v1/exercises                 (custom exercise)
DELETE /api/v1/exercises/{id}            (custom only; 409 if referenced)

GET    /api/v1/templates
POST   /api/v1/templates
GET    /api/v1/templates/{id}
PUT    /api/v1/templates/{id}
DELETE /api/v1/templates/{id}

POST   /api/v1/sessions                  (optionally from templateId)
GET    /api/v1/sessions?from=&to=&cursor=
GET    /api/v1/sessions/{id}
PATCH  /api/v1/sessions/{id}             (end_time, notes)
DELETE /api/v1/sessions/{id}
POST   /api/v1/sessions/{id}/sets        (accepts one set OR a batch)
DELETE /api/v1/sessions/{id}/sets/{setId}
GET    /api/v1/sessions/{id}/suggestions (progressive-overload suggestion)

GET    /api/v1/foods/search?q=           (local-first; USDA fallback, see 4.1)
POST   /api/v1/foods                     (custom food)

GET    /api/v1/meals?date=
POST   /api/v1/meals
DELETE /api/v1/meals/{id}

GET    /api/v1/goals/current
GET    /api/v1/goals                     (history)
PUT    /api/v1/goals                     (closes current, opens new)

GET    /api/v1/reports/volume?weeks=
GET    /api/v1/reports/macros?days=
GET    /api/v1/reports/training-vs-rest?days=
```

**Conventions**
- Error responses are RFC 7807 `application/problem+json` with a consistent
  set of `type` URIs.
- List endpoints paginate. Time-ordered lists (`sessions`, `meals`) use
  **cursor** pagination on `(started_at, id)`; other lists use `page`/`size`.
- Request validation via Bean Validation; violations map to a `400` Problem
  with a `violations` array.
- Auth endpoints are rate-limited (per IP + per account).

### 4.1 Food search resilience `[ADR-005]`

`GET /foods/search` is **local-first**:

1. Query `Food` (CUSTOM owned by the user + any cached USDA rows) and return
   immediately.
2. If fewer than _k_ results, and Redis has no fresh entry for the query,
   call USDA with a hard timeout (~800 ms) and a small connection pool.
3. On USDA success: normalize, upsert into `Food`, cache raw response in
   Redis, merge into the response.
4. On USDA timeout / error / rate-limit: return local results with
   `"externalLookup": "unavailable"` in the payload. Never 5xx because USDA
   is down.

A resilience wrapper (Resilience4j: timeout + circuit breaker + bulkhead)
sits around the USDA client.

---

## 5. Build order

The first milestone is a **walking skeleton** — one full vertical slice, tested
and deployable — before any breadth. This is what de-risks "actually finish
it". `[ADR-013]`

1. **Foundation** — Spring Boot skeleton, Postgres + Flyway, Docker Compose
   (app + db + redis), Actuator, Problem Details handler, JWT access/refresh
   auth, `User` entity with `timezone`.
2. **Walking skeleton** — `Exercise` (seeded) → create `WorkoutSession` →
   add `SetEntry` → `GET /sessions/{id}/suggestions`. Full unit +
   Testcontainers integration tests, GitHub Actions running them, image
   building. Nothing else moves until this is green.
3. **Gym CRUD** — `WorkoutTemplate`, `TemplateExercise`, `exercise_muscle_group`
   seeding, batch set logging, DELETEs, validation.
4. **Progression logic** — harden the suggestion service against the test
   matrix in section 6.
5. **Nutrition CRUD** — `Food`, `MealEntry` (frozen macros), `NutritionGoal`
   (range constraint), USDA integration + two-layer cache + Resilience4j.
6. **Reporting layer** — hand-written volume-by-muscle-group query, macro
   adherence, training-vs-rest. Compute-on-demand with response caching.
7. **Testing pass** — fill gaps: service unit tests (Mockito), repository +
   controller integration tests (Testcontainers), a few `@SpringBootTest`
   happy-path flows.
8. **API docs** — springdoc-openapi; review the generated Swagger UI.
9. *(v1.1, optional)* `@Scheduled` weekly-summary materialization; barcode
   lookup; cardio modality.

---

## 6. Progressive-overload suggestion (detailed spec)

A pure function: `suggest(history: List<ExercisePerformance>, target:
TargetSpec) -> Suggestion`. No database access inside; the controller/service
assembles `history`.

**Inputs**
- `history`: the last _N_ = 3 `workout_session`s (most recent first) in which
  this exercise was performed, each reduced to its **working sets** only
  (`is_warmup = false`).
- `target`: `target_reps` and `target_rpe` from the exercise's
  `template_exercise` row for the current session's template (or, for a
  freeform session, the most recent `template_exercise` for that exercise
  across the user's templates; none → treat as no target).

**Rule (v1)**
- If the exercise has **never** been performed → suggest the target as-is,
  `reason = NO_HISTORY`.
- If it was performed **fewer than N** times → use what exists.
- Let `lastSession` be the most recent performance. If in `lastSession`
  **every working set** reached `reps >= target_reps` **and** (RPE is
  recorded for all sets → `max(rpe) <= 8.0`; RPE missing → treat the rep
  condition alone as sufficient) → **increase load** by 2.5% (upper-body) /
  5% (lower-body), rounded to the nearest 2.5 kg, keep reps at `target_reps`.
  `reason = PROGRESS_LOAD`.
- Else if `lastSession` hit `target_reps` on every set but RPE > 8 →
  **hold load, add one rep**. `reason = PROGRESS_REPS`.
- Else if the **last two** sessions both failed to reach `target_reps`
  on a majority of sets → **deload** 10%. `reason = DELOAD`.
- Else → **repeat** last session's load and reps. `reason = HOLD`.

**Test matrix (must all be covered)**
- never performed; performed once; performed twice
- all sets hit reps, all RPE ≤ 8 → load up
- all sets hit reps, RPE missing → load up
- all sets hit reps, one set RPE 9 → reps up
- first set hits, last set short → hold
- two consecutive short sessions → deload
- upper vs lower body rounding (2.5% vs 5%, round to 2.5 kg)
- exercise logged at `weight_kg = 0` → suggest an extra rep rather than a load bump

---

## 7. Things to be deliberate about (for interview value)

- **Don't build everything as anemic CRUD.** The progression suggestion
  (section 6) and the reporting aggregations (3.3) are real service classes
  with their own unit tests.
- **Write the tricky queries by hand** — volume-by-muscle-group over time is
  a join across `set_entry`, `exercise`, `exercise_muscle_group`,
  `muscle_group`, and `workout_session`, bucketed by ISO week. Understand the
  SQL Hibernate produces.
- **Keep plans and records separate** — `workout_template` vs
  `workout_session`. Nutrition still snapshots point-in-time via frozen
  `MealEntry` macros. `[ADR-003]`, `[ADR-004]`
- **Point-in-time correctness** is a theme worth articulating for nutrition:
  a food log is a historical record, so recomputing it from mutable reference
  data is a bug. Sessions take the lighter approach (`[ADR-003]`).
- **Postgres beyond CRUD** — range types + exclusion constraint for goals,
  `citext` for email, `ON CONFLICT` upserts for the USDA cache.
- **Resilience** — an external dependency in the request path is designed for
  failure, not assumed up (section 4.1).
- **Document decisions as you go** — `DECISIONS.md` (ADR-style) is the
  companion to this doc.

---

## 8. Known limitations / deferred

- Strength training only; no cardio, distance, or time-based work.
- No supersets / circuits / drop sets as first-class structures.
- JWT refresh tokens are revocable; access tokens are not (short TTL
  mitigates).
- Training-vs-rest is descriptive, not inferential — small n, uncontrolled.
- Weekly-summary materialization, barcode lookup: v1.1.
