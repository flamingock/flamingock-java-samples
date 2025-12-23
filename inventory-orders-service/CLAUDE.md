# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an example project demonstrating Flamingock, a Change-as-Code execution engine. It simulates an e-commerce inventory & orders service that coordinates versioned changes across multiple target systems: MongoDB, Kafka Schema Registry, and LaunchDarkly (used here as an external configuration system, not runtime flag evaluation).

**What Flamingock is:** A Change-as-Code execution engine that safely, deterministically, and auditably applies versioned changes to external systems at application startup. The goal is to ensure that all dependent external systems evolve in a synchronized and auditable way with the application. Think "Liquibase for all external systems, with auditability and safety as first-class constraints."

**What Flamingock is NOT:** Not a database migration tool, not Infrastructure-as-Code (Terraform/Pulumi), not a CI/CD pipeline, not runtime feature flagging.

## Build Commands

```bash
./gradlew build          # Build the project
./gradlew run            # Run the application (executes pending changes at startup)
./gradlew test           # Run integration tests with Testcontainers (no external services needed)
./gradlew clean          # Clean build artifacts
```

## Running the Application

**Option 1: With Docker Compose (for manual execution)**
```bash
docker-compose up -d     # Start MongoDB, Kafka, Schema Registry, LaunchDarkly mock
./gradlew run            # Execute pending changes
```

**Option 2: With Testcontainers (for testing)**
```bash
./gradlew test           # Starts all containers automatically via Testcontainers
```

## Architecture

### Flamingock Core Concepts

- **Change** - The atomic unit of change. Defines what system it targets, what version it represents, and how it is applied. Changes are versioned, ordered, immutable once applied, and should be idempotent.
- **Audit Store** - Records execution state (STARTED, COMPLETED). Intentionally separate from target systems to enable safe retries, manual intervention, and cross-system governance.
- **Target System** - The external system being modified. Flamingock does not assume target systems are transactional; safety is enforced via execution tracking.

### This Project's Setup

- Entry point: `InventoryOrdersApp.java` with `@EnableFlamingock` annotation
- Changes discovered from `io.flamingock.examples.inventory.changes` package
- Uses compile-time annotation processing via `flamingock-processor`

### Target Systems (defined in `TargetSystems.java`)
1. **mongodb-inventory** - MongoDB operations (also used as Audit Store in this example; in real setups, the Audit Store may be a dedicated system)
2. **kafka-inventory** - Kafka Schema Registry operations
3. **toggle-inventory** - LaunchDarkly as external configuration system (not runtime flag evaluation)

### Change Naming Convention
In this example repository, changes follow the pattern: `_NNNN__[target]_[description].java`
- Example: `_0001__mongodb_addDiscountCodeFieldToOrders.java`
- Auto-ordered by numeric prefix
- Each change class has an `@Apply` method and may optionally define a `@Rollback` for reconciliation or manual recovery

### Utility Classes (`util/` package)
- `MongoDBUtil` - MongoDB connection string builder
- `KafkaSchemaManager` - Schema Registry REST operations
- `LaunchDarklyClient` - Feature flag CRUD via REST API
- `ConfigFileManager` - YAML config file handling

## Technology Stack
- Java 17
- Flamingock v1.0.0-beta.5
- MongoDB Driver Sync v5.5.1
- Apache Kafka v3.7.0 with Avro v1.11.3
- Confluent Schema Registry Client v7.5.0
- OkHttp3 v4.12.0 (for REST calls)
- Testcontainers v2.0.2 (for integration tests)

## Infrastructure Services (docker-compose.yml)
| Service           | Port  | Purpose                                       |
|-------------------|-------|-----------------------------------------------|
| MongoDB           | 27017 | Target system & Audit Store (in this example) |
| Kafka             | 9092  | Event streaming                               |
| Schema Registry   | 8081  | Avro schema management                        |
| LaunchDarkly Mock | 8765  | External configuration system simulation      |

## Testing
Integration tests use Testcontainers to spin up isolated Docker containers:
- `SuccessExecutionTest.java` tests all 7 changes
- Tests verify MongoDB documents, Kafka schemas, and audit log entries
- No external Docker Compose required for tests
