# Feature Flags with Flamingock + Spring Boot + PostgreSQL

A working example that builds a feature-flag service from scratch using [Flamingock](https://www.flamingock.io) to manage database schema evolution, Spring Boot for the REST API, and PostgreSQL as the backing store.

## Prerequisites

- Java 21 (use `sdk env` if you have [SDKMAN](https://sdkman.io/) installed)
- Docker & Docker Compose

## Quick Start

```bash
docker compose up --build
```

That's it. Postgres starts first (health-checked), then the app boots, Flamingock runs the migrations, and the API is live at `http://localhost:8080`.

### Running locally (without Docker for the app)

```bash
# Start only Postgres
docker compose up db -d

# Run the app
./gradlew bootRun
```

## API

### Create a flag

```bash
curl -s -X POST localhost:8080/flags \
  -H "Content-Type: application/json" \
  -d '{"name":"dark-mode","description":"Dark mode UI"}'
```

### List all flags

```bash
curl -s localhost:8080/flags
```

### Update a flag (enable + set rollout %)

```bash
curl -s -X PUT localhost:8080/flags/dark-mode \
  -H "Content-Type: application/json" \
  -d '{"enabled":true,"rolloutPercentage":30}'
```

### Evaluate a flag for a user

```bash
curl -s "localhost:8080/flags/evaluate/dark-mode?userId=user-42"
```

Evaluation is deterministic — the same `userId` always lands in the same rollout bucket (SHA-256 hash).

### Add a targeting rule

```bash
curl -s -X POST localhost:8080/flags/dark-mode/rules \
  -H "Content-Type: application/json" \
  -d '{"attribute":"plan","operator":"equals","value":"pro"}'
```

### Evaluate with attributes

```bash
curl -s "localhost:8080/flags/evaluate/dark-mode?userId=user-999&plan=pro"
```

When a targeting rule matches, the flag is enabled regardless of rollout percentage.

### List rules for a flag

```bash
curl -s localhost:8080/flags/dark-mode/rules
```

## How Flamingock manages the schema

Instead of `ddl-auto` or hand-written SQL scripts, Flamingock applies versioned, auditable changes at startup:

| Change | What it does |
|--------|-------------|
| `_0001__CreateFlagsTable` | Creates the `feature_flags` table |
| `_0002__AddRolloutPercentage` | Adds the `rollout_percentage` column |
| `_0003__CreateTargetingRules` | Creates the `targeting_rules` table + index |

Each change targets the `postgres-flags` SQL target system and receives a `java.sql.Connection` automatically. Flamingock tracks execution in its audit store so changes run exactly once, even across restarts.

## Targeting rule operators

| Operator | Behaviour |
|----------|-----------|
| `equals` | Exact string match |
| `contains` | Substring match |
| `in` | Comma-separated list membership |
| `starts_with` | Prefix match |

## Project structure

```
feature-flags-example/
├── docker-compose.yml
├── Dockerfile
├── build.gradle
├── settings.gradle
└── src/main/java/com/example/flags/
    ├── FeatureFlagApplication.java      # @EnableFlamingock entry point
    ├── config/FlamingockConfig.java      # SqlTargetSystem + audit store beans
    ├── changes/                          # Flamingock migrations
    ├── model/                            # JPA entities
    ├── repository/                       # Spring Data repositories
    ├── service/EvaluationService.java    # Flag evaluation logic
    └── controller/FlagController.java    # REST API
```
