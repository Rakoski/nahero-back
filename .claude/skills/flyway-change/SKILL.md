---
name: flyway-change
description: >-
  Plan and write a Flyway migration for nahero-back safely. Use whenever a change
  touches the database schema or stored data: new table, new column, new index,
  rename, type change, NOT NULL, unique constraint, enum value, backfill, or data
  fix. Also use when reviewing a migration someone already wrote, or when a test
  fails with a schema error. Enforces forward-only migrations, the
  backfill-then-constrain split, and a Testcontainers test that boots the full
  chain.
---

# flyway-change

Migrations are the only source of schema in this project. There is no
`spring.jpa.hibernate.ddl-auto` in `src/main/resources/application.properties`
and no Flyway configuration either, so Flyway runs on defaults: every
`src/main/resources/db/migration/V<n>__<desc>.sql` is applied in order at
startup, and Hibernate never creates or alters anything. If it is not in a
migration, it does not exist in production.

Community Flyway has no undo. Every migration is forward-only: a mistake is
fixed by writing `V<n+1>`, never by editing `V<n>`.

## Before writing anything

1. List the existing migrations and take the highest number:
   `ls -1 src/main/resources/db/migration/ | sed 's/V\([0-9]*\)__.*/\1/' | sort -n | tail -1`
   The new file is that number plus one. Do not guess from the directory
   listing order — it sorts lexicographically, so `V9` appears after `V26`.
2. Read the migrations that already touch the same table. Schema history here is
   not linear: `processed_stripe_events` was renamed to `payment_events` in
   `V19`, `difficulty_level` was dropped from `practice_exams` in `V7`, and
   several columns were widened or constrained after the fact. Assume the
   current shape of a table is the sum of all its migrations, not what `V1` says.
3. Check the JPA entity that maps the table. Column adds that the entity does
   not know about are silently ignored; column renames and type changes that the
   entity still maps the old way fail at runtime, not at migration time.

## Naming

`V<n>__<snake_case_description>.sql`, lowercase, no dates, double underscore
after the version. Match the existing voice: `create_*` for new tables,
`add_*_to_*` for columns, `drop_*_from_*`, `rename_*_to_*`, `backfill_*`,
`make_*_not_null`.

## The rules that came from actual breakage

**A NOT NULL column on a populated table is two migrations, never one.**
`V4__insert_not_null_passing_score`, `V13__make_exams_difficulty_level_not_null`
and `V24__backfill_email_confirmed_at_for_existing_users` all exist because the
constraint and the data landed separately. The order is:

- `V<n>`: add the column nullable, or add the column with a DEFAULT.
- `V<n>`: backfill existing rows in the same or a following migration.
- `V<n+1>`: `ALTER COLUMN ... SET NOT NULL`.

Adding a NOT NULL column with no default to a table that has rows fails
outright. Adding one *with* a default rewrites nothing on Postgres 11+ but
still needs the application deployed before or with it, depending on which side
writes first.

**A rename is a breaking change to more than the schema.** Renaming a table or
column breaks the JPA entity mapping, any QueryDSL Q-class (regenerate), any
raw SQL or `JdbcTemplate` call, and any response field derived from it. Before
renaming, grep for the old name across `src/main/java` and, if it surfaces in a
response DTO, run the `contract-check` skill.

**A deploy is not atomic.** The migration runs at application startup, so for a
window the old code can see the new schema. Prefer additive changes: add the new
column, write to both, drop the old one in a later release. Do not combine
"add new column" and "drop old column" in the same migration when the old column
is still read anywhere.

**Data fixes are migrations too.** A one-off `UPDATE` belongs in a numbered
migration, not in a psql session against production. It must be idempotent in
effect: scope it with a `WHERE` clause that excludes rows already fixed, the way
`V24` does.

**Indexes.** `V6__add_indexes_to_main_tables` set the precedent for plain
`CREATE INDEX`. Note that `CREATE INDEX CONCURRENTLY` cannot run inside a
transaction and Flyway wraps Postgres migrations in one, so do not reach for
`CONCURRENTLY` without first confirming it actually works in this setup. On the
current table sizes a plain `CREATE INDEX` is fine; revisit when a table is
large enough that the write lock matters.

**Enums.** Backend enums are persisted as strings. Adding a value needs no
migration, but removing or renaming one orphans existing rows — write the
backfill. A new value is also a frontend contract change; see `contract-check`.

## Verification

A migration is not done until the full chain boots from empty. That is what the
`@SpringBootTest` integration tests already do: `TestConfig` starts a
`postgres:14.3` Testcontainer and points the datasource at it, so every
integration test run replays `V1` through the new migration against a fresh
database. A migration that only works against your local dev database — which
has the old shape plus whatever you patched by hand — proves nothing.

So: add or extend an integration test that reads or writes the changed column,
following the pattern in
`src/test/java/br/com/naheroback/modules/reengagement/useCases/dispatchReengagementEmails/DispatchReengagementEmailsIntegrationTest.java`
(`@SpringBootTest`, schedulers disabled via `@TestPropertySource`, external
services replaced with `@MockitoBean`).

Do not run the suite yourself — it is slow and the user compiles. Hand over the
command instead:

```
./mvnw test -Dtest=<TestClass>
```

## Checklist before saying it is done

- [ ] New file, next sequential version, nothing edited that was already shipped
- [ ] Nullable-add and NOT NULL are separate migrations if the table has rows
- [ ] Backfill scoped so rerunning it changes nothing extra
- [ ] JPA entity updated; QueryDSL Q-classes regenerate on build
- [ ] Old-name grep clean after a rename
- [ ] `contract-check` run if any `*Response` field is affected
- [ ] Integration test named that exercises the change, command handed to the user
