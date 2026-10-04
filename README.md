# Nahero — Backend API

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)
![Status](https://img.shields.io/badge/status-in%20production-success)

This is the Spring Boot API powering **[nahero.site](https://nahero.site)** — a practice-exam platform for cloud and IT certifications.

**Nahero is live and running in production.** It is not a demo, a course project, or a portfolio mock-up: it serves real students taking real practice exams, it processes real payments through Stripe, and it has paying subscribers on recurring plans. Everything described below is code that runs against production traffic today.

The Next.js frontend lives in [nahero-front](https://github.com/Rakoski/nahero-front).

---

## What it does

Nahero lets students practice for certification exams under realistic conditions and see exactly where they stand.

- **Timed practice attempts** — start, save progress, resume across sessions, finish, abandon, or time out. Questions are shuffled per attempt and the shuffle order is persisted so a resumed attempt looks identical to the one the student left.
- **Scored results with domain breakdown** — every question is tagged with an exam domain, so results show not just a pass/fail score but which domains need work.
- **Student dashboard and history** — aggregate performance over time, plus a full attempt history.
- **Free and premium tiers** — free practice exams for everyone; premium unlocks the full question banks via a Stripe subscription.
- **Subscriptions and billing** — Stripe Checkout, webhook handling with idempotent event processing, subscription status, cancellation (immediate or at period end), and a reconciler that repairs drift between Stripe and the local database.
- **Multi-language content** — questions and attempts carry a language, and API error messages are localized (`en` / `pt`).
- **Accounts** — JWT auth, email verification, password recovery, role and permission model, UTM attribution captured at signup.
- **Re-engagement emails** — scheduled dispatch runs that email inactive students, with per-email status tracking and one-click unsubscribe.

## Tech stack

| Layer | Choice |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.4 (Web, Security, Data JPA, Data JDBC) |
| Auth | JWT (`com.auth0:java-jwt`) behind a servlet filter |
| Database | PostgreSQL, schema managed by Flyway (31 migrations and counting) |
| Queries | QueryDSL for type-safe non-trivial queries |
| Mapping | ModelMapper + `BeanCopyUtils` |
| Messaging | Spring Kafka |
| Payments | Stripe (Checkout, webhooks, reconciliation) |
| Testing | JUnit 5 + Testcontainers (real Postgres and Kafka, no mocks for infra) |
| Packaging | Docker / Docker Compose |

## Architecture

The codebase is organised by **feature module**, and every module follows the same shape. Business logic lives in **one use case per action** — controllers stay thin and do nothing but validate input and delegate.

```
src/main/java/br/com/naheroback/
├── common/          # cross-cutting: security config, BaseEntity, exception handling, utils
├── modules/
│   ├── auth/            # login, refresh, email verification, password recovery
│   ├── user/            # accounts, roles, permissions
│   ├── practiceExams/   # exams, questions, alternatives, answers, student attempts
│   ├── subscription/    # plan status, cancellation
│   ├── payment/         # payment events, idempotency
│   ├── enrollment/
│   ├── exams/
│   ├── flashcards/
│   └── reengagement/    # inactivity email campaigns
└── providers/
    ├── payment/stripe/  # StripeService, webhook verifier, reconciler
    └── storage/
```

A module looks like this:

```
modules/<feature>/
├── controllers/   # @RestController — thin, delegates to use cases
├── entities/      # JPA entities
├── repositories/  # Spring Data + QueryDSL
├── services/      # shared per-module services
└── useCases/      # one folder per action: <Action>Request, <Action>Response, <Action>UseCase
```

For example, `practiceExams/useCases/studentPracticeAttempt/` contains `create`, `saveProgress`, `getInProgress`, `getState`, `finish`, `abandon`, `timeout`, `getResult`, `getHistory`, and `getDashboardSummary` — each one an isolated, testable unit.

## Getting started

### Prerequisites

- Java 21
- Docker and Docker Compose
- Maven (or use the bundled `./mvnw` wrapper)

### Run it

```bash
cp .env.example .env     # fill in DB, JWT and Stripe values
docker compose up -d     # starts the API and PostgreSQL
```

Or run the API directly against a local Postgres:

```bash
./mvnw clean install
./mvnw spring-boot:run
```

### Tests

Integration tests spin up real Postgres and Kafka containers via Testcontainers, so Docker must be running.

```bash
./mvnw test                                  # everything
./mvnw test -Dtest=ClassName                 # one class
./mvnw test -Dtest=ClassName#methodName      # one method
```

## Contributing

Contributions are welcome. A few conventions this codebase holds to:

- **One use case per action.** No business logic in controllers.
- **Never edit a Flyway migration that has shipped** — add a new `V<n>__<description>.sql`.
- **Never commit secrets.** Credentials come from environment variables.
- Throw the custom exceptions in `common/exceptions/custom/`; `GlobalExceptionHandler` maps them to HTTP responses.
- User-facing messages belong in `src/main/resources/locales/`, in every supported language.
- Response shapes are consumed by the frontend — breaking one requires a coordinated change in `nahero-front`.

## License

Copyright (C) 2026 Nahero.

Licensed under the **GNU Affero General Public License v3.0**. See [LICENSE](LICENSE) for the full text.

The AGPL is deliberate. Nahero is a network service, and section 13 means anyone who runs a modified version of this code as a public service must make their source available to its users under the same terms. You are free to use, study, modify, and self-host this software — you just can't take it closed-source and run it as a competing hosted product.
