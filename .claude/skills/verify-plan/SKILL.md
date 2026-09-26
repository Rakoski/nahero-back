---
name: verify-plan
description: >-
  State how a change will be proven before writing it. Use at the start of any
  non-trivial backend change — a new use case, a change to an existing use case,
  a scheduler, a payment or email flow, a migration, a security-relevant fix. Also
  use when a change is already written but nobody can say what proves it works.
  Produces a short plan naming the test that would fail today, the infra it needs,
  and what is deliberately out of scope.
---

# verify-plan

The premise: decide what would prove the change works before writing the change.
Not a test-first mandate — a claim, written down, that can be checked later. If
you cannot name what would fail today and pass afterwards, you do not yet
understand the change.

Keep the output short. This is a plan, not a document. It rots the moment the
code lands, so it lives in the conversation and in the test names, not in a file.

## Output shape

**Change** — one sentence on what behaviour changes.

**Proof** — the specific test that fails today and passes after. Name the class
and method you will add or extend. If the answer is "no test", say so and say
why, out loud.

**Infra it needs** — unit (Mockito, no context) or integration (`@SpringBootTest`,
real Postgres)? Which external services get mocked?

**Contracts touched** — any `*Response`/`*Request`/route/enum/error code change
means running `contract-check`. Any schema or stored-data change means running
`flyway-change`.

**Out of scope** — what this change deliberately does not cover. Stating it
prevents the scope from drifting mid-implementation.

## Picking the tier

Sixteen test classes exist; both tiers are already represented, so follow the
one that fits rather than inventing a third.

**Unit test** — `*UseCaseTest`, plain JUnit 5 plus Mockito, no Spring context.
Correct when the logic under test is branching and decision-making and the
collaborators are uninteresting. Fast enough to run constantly. See
`DispatchReengagementEmailsUseCaseTest`, `ForgotPasswordUseCaseTest`.

**Integration test** — `*IntegrationTest`, `@SpringBootTest`, real Postgres via
Testcontainers. Correct when the thing that could break is the database: a
query, a migration, a JPA mapping, a unique constraint, a transaction boundary,
a QueryDSL predicate. A mocked repository cannot fail the way a real one does,
so a unit test here proves nothing.

Default to integration whenever the risk is in the query or the schema. That is
where this project's actual defects live.

## The integration-test pattern already in the repo

`DispatchReengagementEmailsIntegrationTest` is the reference. Copy its shape:

- `@SpringBootTest` on the class. `TestConfig` starts a `postgres:14.3`
  Testcontainer and rewrites the datasource properties, so the full Flyway chain
  replays from empty on every run.
- Disable schedulers you are not testing via `@TestPropertySource`:
  `reengagement.enabled=false`, `payment.reconcile.enabled=false`. Otherwise a
  background job races your assertions.
- Replace anything that reaches the outside world with `@MockitoBean` —
  `EmailService`, Stripe. Never let a test send mail or hit a payment provider.
- Do not assume an empty or exclusively-yours database. Generate unique
  fixture data (`"inactive-%s@example.com".formatted(System.nanoTime())`) and
  clean up in `@AfterEach`.
- Reach for `JdbcTemplate` when you need to assert on a table the JPA layer does
  not expose, or to set up state the entity cannot express.

## Rate limits, schedulers, and time

Several features are time- or count-gated: `rate-limit.email-dispatch.*`,
`reengagement.min-days-between-emails`, `reengagement.campaign-months`,
`payment.reconcile.lookback-hours`. Never verify those by waiting. Override the
property in `@TestPropertySource`, or write the timestamp the test needs directly
into the row. A test that sleeps is a test that will be deleted for being flaky.

## Running it

Do not run the suite yourself — it is slow and the user compiles. Name the
command and hand it over:

```
./mvnw test -Dtest=<TestClass>
./mvnw test -Dtest=<TestClass>#<method>
```

Integration tests need Docker up for Testcontainers.

## When the honest answer is "no automated test"

Some things are not worth a test: a log line, a config default, an error message
string. Say that plainly and name the manual check instead — the endpoint to
curl, the page in `../nahero-front` to open, the log line to look for. An
explicit manual check is a valid plan. An unstated one is how regressions get in.
