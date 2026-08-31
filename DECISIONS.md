# Architecture Decisions

ADR-style log of design decisions with trade-offs. Each entry is short: the
decision, why, and what was given up. Referenced from `DESIGN.md` as
`[ADR-NNN]`.

Status values: **Accepted**, **Superseded by ADR-NNN**, **Revisited**.

---

## ADR-001 — Exercise-to-muscle-group is a weighted many-to-many

**Date:** 2026-08-31 · **Status:** Accepted

**Context.** The reporting feature "weekly training volume trend per muscle
group" needs to attribute each set's volume to muscles. A single
`Exercise.muscle_group` column can't represent compound lifts — a bench press
trains chest, triceps, and front delts — so that report would be wrong for
exactly the lifts that matter most.

**Decision.** Model `ExerciseMuscle(exercise_id, muscle_group, role,
contribution)` where `contribution` is a 0–1 weight. Per-muscle volume for a
set is `reps × weight × contribution`. `muscle_group` is an enum, not a table
or free text. Seed data follows the convention that contributions sum to ~1.0
per exercise, but this is not enforced.

**Alternatives considered.**
- `primary_muscle` + `secondary_muscles[]` — simpler, but "secondary" is
  binary and can't express that triceps get ~25% of a bench press vs ~15% for
  front delts. Reports would over-count secondary muscles.
- Single enum column — rejected, see Context.

**Trade-offs.** More seed-data effort (every seeded exercise needs a
contribution breakdown, which is somewhat subjective). One extra join in the
volume query. Accepted because the weighted model is the feature's whole point
and is a good data-modeling talking point.

---

## ADR-002 — All event timestamps are `timestamptz` UTC; calendar dates are derived per-user

**Date:** 2026-08-31 · **Status:** Accepted

**Context.** Nearly every report buckets events into days: "meals on date X",
"training days vs rest days", "trailing 7-day adherence". If a naive date is
stored, or the day boundary is computed in the server's timezone, an 11pm meal
lands in the wrong day and every downstream aggregate is subtly off.

**Decision.** Store every user-event timestamp as `timestamptz` in UTC. Add
`User.timezone` (IANA string). Derive local dates in queries as
`date(ts AT TIME ZONE user.timezone)`. Remove the previously proposed stored
`WorkoutSession.date` column — it was redundant with `started_at` and a source
of drift.

**Trade-offs.** Slightly more complex queries (timezone expression in GROUP
BY, can't index a stored date directly — mitigated with expression indexes if
needed). Users travelling across timezones will see historical days bucketed
by their *current* timezone; acceptable for a personal tracker.

---

## ADR-003 — Sessions snapshot their planned structure into `SessionExercise`

**Date:** 2026-08-31 · **Status:** Accepted

**Context.** A `WorkoutSession` may be created from a `WorkoutTemplate`. If the
session only holds `template_id` and reads targets from the live template,
then editing or deleting that template later rewrites what past sessions were
"planned" to be, breaking planned-vs-actual analysis.

**Decision.** On session creation from a template, copy the template's
per-exercise targets into `SessionExercise` rows (`planned_sets`,
`planned_reps_min/max`, `planned_rpe`). The session is thereafter
self-contained. Freeform sessions create `SessionExercise` rows on demand with
null planned values. `SetEntry` hangs off `SessionExercise`, not the session
directly.

**Trade-offs.** Data duplication between `TemplateExercise` and
`SessionExercise`. One more table. Accepted — it is the same plan-vs-record
principle the domain already commits to for templates vs sessions, and it is
cheap.

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

## ADR-006 — API is versioned, user-scoped at the repository layer, and uses RFC 7807

**Date:** 2026-08-31 · **Status:** Accepted

- **Versioning:** all routes under `/api/v1`. Free now, painful to retrofit.
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

## ADR-010 — Drop `Exercise.is_custom`

**Date:** 2026-08-31 · **Status:** Accepted

`is_custom` is exactly `created_by IS NOT NULL`. Storing it invites the two
fields disagreeing. Derive it in the DTO. (If a hot query ever needs it
indexed, revisit as a denormalization with a documented reason.)

---

## ADR-011 — Bodyweight-exercise volume uses added load plus bodyweight

**Date:** 2026-08-31 · **Status:** Accepted

**Context.** Pull-ups and dips logged with `weight_kg = 0` would contribute
zero volume, understating training load badly.

**Decision.** `Exercise.is_bodyweight` flag. For flagged exercises,
`effective_weight_kg = weight_kg (added load) + session.bodyweight_kg`, falling
back to the user's most recent known bodyweight. If no bodyweight is known, the
set contributes 0 and the response flags it so the client can prompt.

**Trade-offs.** Volume for bodyweight work depends on a bodyweight estimate
that may be stale. Accepted as far better than zero; users are nudged to log
bodyweight.

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
expression-based date bucketing, and `ON CONFLICT`. H2's compatibility modes
don't cover these, so an H2-backed test would pass against code that fails in
production. `@DataJpaTest` and controller integration tests use a
Testcontainers Postgres matching the prod major version. Trade-off: tests need
Docker available and are a few seconds slower to start.
