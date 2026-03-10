# Feature Flags with Kill Switch and Scheduled Activation — Flamingock + Spring Boot + PostgreSQL

A working example that extends the feature-flag service with a **kill switch** and **scheduled flag activation**, using [Flamingock](https://www.flamingock.io) to manage every schema change, Spring Boot for the REST API, and PostgreSQL as the backing store.

This project builds on the [feature-flags](../feature-flags/readme.md) example. The two new Flamingock changes add the `force_disabled` kill-switch column and the `activate_at`/`deactivate_at` scheduling columns.

## Prerequisites

- Java 21 (use `sdk env` if you have [SDKMAN](https://sdkman.io/) installed)
- Docker & Docker Compose

## Quick Start

```bash
docker compose up --build
```

Postgres starts first (health-checked), then the app boots, Flamingock runs all five migrations, and the API is live at `http://localhost:8080`.

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

### Activate the kill switch

```bash
curl -s -X PUT localhost:8080/flags/dark-mode \
  -H "Content-Type: application/json" \
  -d '{"forceDisabled":true}'
```

Once the kill switch is on, the flag evaluates to `false` for every user — targeting rules, rollout percentage, and scheduling are all bypassed. The reason returned is `"kill switch active"`.

### Deactivate the kill switch

```bash
curl -s -X PUT localhost:8080/flags/dark-mode \
  -H "Content-Type: application/json" \
  -d '{"forceDisabled":false}'
```

### Schedule a flag

```bash
curl -s -X PUT localhost:8080/flags/dark-mode \
  -H "Content-Type: application/json" \
  -d '{"enabled":true,"activateAt":"2025-11-28T00:00:00Z","deactivateAt":"2025-11-29T00:00:00Z"}'
```

The flag will evaluate to `false` before `activateAt` (`"not yet active"`) and after `deactivateAt` (`"schedule expired"`). During the window, normal evaluation applies.

## How Flamingock manages the schema

Instead of `ddl-auto` or hand-written SQL scripts, Flamingock applies versioned, auditable changes at startup:

| Change | What it does |
|--------|-------------|
| `_0001__CreateFlagsTable` | Creates the `feature_flags` table |
| `_0002__AddRolloutPercentage` | Adds the `rollout_percentage` column |
| `_0003__CreateTargetingRules` | Creates the `targeting_rules` table + index |
| `_0004__AddKillSwitch` | Adds the `force_disabled` column to `feature_flags` |
| `_0005__AddScheduledActivation` | Adds the `activate_at` and `deactivate_at` columns |

Each change targets the `postgres-flags` SQL target system and receives a `java.sql.Connection` automatically. Flamingock tracks execution in its audit store so changes run exactly once, even across restarts.

## Evaluation logic

Flags are evaluated in priority order:

1. **Kill switch** — if `force_disabled` is `true`, returns `false` immediately.
2. **Disabled** — if `enabled` is `false`, returns `false`.
3. **Not yet active** — if `activate_at` is set and now is before it, returns `false`.
4. **Schedule expired** — if `deactivate_at` is set and now is past it, returns `false`.
5. **Targeting rules** — if any rule matches the user's attributes, returns `true`.
6. **Rollout bucket** — deterministic SHA-256 hash of `flagName:userId` maps the user to a 0–99 bucket; returns `true` if the bucket is below `rolloutPercentage`.

## Targeting rule operators

| Operator | Behaviour |
|----------|-----------|
| `equals` | Exact string match |
| `contains` | Substring match |
| `in` | Comma-separated list membership |
| `starts_with` | Prefix match |

## Project structure

```
feature-flags-kill-switch/
├── docker-compose.yml
├── Dockerfile
├── build.gradle
├── settings.gradle
└── src/main/java/io/flamingock/flags/
    ├── FeatureFlagApplication.java         # @EnableFlamingock entry point
    ├── config/FlamingockConfig.java         # SqlTargetSystem + audit store beans
    ├── changes/                             # Flamingock migrations
    │   ├── _0001__CreateFlagsTable.java
    │   ├── _0002__AddRolloutPercentage.java
    │   ├── _0003__CreateTargetingRules.java
    │   ├── _0004__AddKillSwitch.java        # kill switch column
    │   └── _0005__AddScheduledActivation.java  # scheduled activation columns
    ├── model/                               # JPA entities
    ├── repository/                          # Spring Data repositories
    ├── service/
    │   └── EvaluationService.java           # Flag evaluation logic
    └── controller/FlagController.java       # REST API
```
