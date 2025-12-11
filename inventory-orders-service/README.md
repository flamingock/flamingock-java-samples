<p align="center">
  <img src="../misc/logo-with-text.png" width="420px" alt="Flamingock logo" />
</p>
___

# Inventory & Orders Service Example

## Example Overview

This example simulates an **e-commerce service** that manages inventory and orders. It demonstrates how Flamingock coordinates multiple target systems in lockstep using the **Change-as-Code** approach.

The story begins when the **marketing team** launches a promotional campaign that requires support for **discount codes**.
To implement this feature safely, the product and engineering teams plan a sequence of deployments, each introducing incremental changes in a controlled, auditable way.

As development progresses, the **sales team** also requests the ability to quickly search and report on orders by discount code.
This leads the engineers to add an index on the new field as part of the rollout, ensuring the system remains both functional and performant.

### Lifecycle of the feature rollout

1. **Initial deployment**  <a id="initial-deployment"></a>
   *Business driver:* prepare the system for discounts while keeping the feature hidden.
   - Add the `discountCode` field to the `orders` collection in **MongoDB**.
   - Update the `OrderCreated` schema in **Kafka** to include the same field.
   - Create feature flags for discount functionality via **LaunchDarkly Management API**.
   - Deploy application code that can *handle* the new field and flag, but does not yet *use* them.
   - **Associated Flamingock changes:**
      - [`AddDiscountCodeFieldToOrders`](#adddiscountcodefieldtoorders)
      - [`UpdateOrderCreatedSchema`](#updateordercreatedschema)
      - [`AddFeatureFlagDiscounts`](#addfeatureflagdiscounts)

2. **Second deployment** <a id="second-deployment"></a>
   *Business driver:* ensure existing and new orders remain consistent.
   - Backfill existing orders with a default `discountCode` (e.g. `"NONE"`).
   - Application logic begins to populate the field for new orders, still hidden behind the flag.
   - Add an index on the `discountCode` field for efficient reporting queries, requested by the sales team.
   - **Associated Flamingock changes:**
      - [`BackfillDiscountsForExistingOrders`](#backfilldiscountsforexistingorders)
      - [`AddIndexOnDiscountCode`](#addindexondiscountcode)

3. **Runtime activation (no deployment)** <a id="runtime-activation"></a>
   *Business driver:* marketing activates discounts for customers.
   - The feature flag is enabled at runtime using a feature-flag tool (e.g. Unleash, LaunchDarkly).
   - No redeployment is required — the system is already prepared.

4. **Final deployment** <a id="final-deployment"></a>
   *Business driver:* make the discounts feature permanent and clean up temporary scaffolding.
   - Archive temporary feature flags via LaunchDarkly Management API using Flamingock.
   - Remove the conditional `if (flag)` logic from the application code.
   - The discounts feature is now permanent and the system has been fully cleaned up.
   - **Associated Flamingock changes:**
      - [`CleanupFeatureFlagDiscounts`](#cleanupfeatureflagdiscounts)
      - [`CleanupOldSchemaVersion`](#cleanupoldschemaversion)

### What this demonstrates

This example showcases Flamingock’s ability to:
- Introduce, evolve, and later clean up **multiple target systems** (databases, event schemas, and configuration files).
- Support the **realistic lifecycle of a feature rollout**, spanning multiple deployments.
- Keep system evolution **controlled, auditable, and aligned with application code changes**.

---

## Table of Contents

- [Target Systems](#target-systems)
- [Complementary Stack](#complementary-stack)
- [Prerequisites](#prerequisites)
- [Dependencies](#dependencies)
- [How to Run this Example](#how-to-run-this-example)
   - [Option 1: Run the Application (Recommended)](#option-1-run-the-application-recommended)
   - [Option 2: Run Tests](#option-2-run-tests)
   - [Option 3: Run with GraalVM Native Image (Optional)](#option-3-run-with-graalvm-native-image-optional)
- [Proven Functionalities](#proven-functionalities)
- [Implemented Changes](#implemented-changes)
- [Contributing](#contributing)
- [Get Involved](#get-involved)
- [License](#license)

---

## Target Systems

This example coordinates changes across three different target systems:

1. **MongoDB** - Orders collection (also used as AuditStore)
2. **Kafka + Schema Registry** - Event schemas for order events
3. **LaunchDarkly Management API** - Feature flag creation/deletion via REST API

## Complementary Stack

- Java 17+ (required)
- Gradle (wrapper included)
- Docker Compose (to run MongoDB, Kafka, and Schema Registry locally)
- IntelliJ IDEA (recommended IDE with full support)

## Prerequisites

Before running this example, ensure you have installed:
- **Java 17 or higher** (required - this project uses Java 17 features)
- Docker and Docker Compose
- Git

For IntelliJ IDEA users: The project includes IntelliJ-specific configurations for optimal development experience.

## Dependencies

### Flamingock dependencies
```kotlin
implementation(platform("io.flamingock:flamingock-community-bom:$flamingockVersion"))
implementation("io.flamingock:flamingock-community")
annotationProcessor("io.flamingock:flamingock-processor:$flamingockVersion")
```

### Other key dependencies
```kotlin
// MongoDB
implementation("org.mongodb:mongodb-driver-sync:3.7.0")

// Kafka & Schema Registry
implementation("org.apache.kafka:kafka-clients:3.7.0")
implementation("io.confluent:kafka-schema-registry-client:7.5.0")
implementation("org.apache.avro:avro:1.11.3")

// HTTP client for LaunchDarkly Management API
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// YAML config management
implementation("org.yaml:snakeyaml:2.2")
```

Check out the [compatibility documentation](https://docs.flamingock.io) for using Flamingock with MongoDB and Kafka.

## How to Run this Example

### Feature Flag Workflow

**Important**: This example demonstrates Flamingock's role in the feature flag lifecycle:
1. **Flamingock creates** the flag structure (disabled by default) for safe deployment
2. **Teams manage** the flag's runtime state through their feature flag tool (LaunchDarkly, Unleash, etc.)
3. **Flamingock removes** the flag when the feature becomes permanent (same commit as code cleanup)

### Option 1: Run the Application (Recommended)

1. **Clone the Flamingock examples repository:**
```bash
git clone https://github.com/flamingock/flamingock-java-examples.git
cd flamingock-java-examples/inventory-orders-service
```

2. **Start the infrastructure with Docker Compose:**
```bash
docker-compose up -d
```

This starts:
- MongoDB on port 27017
- Kafka on port 9092
- Zookeeper on port 2181
- Schema Registry on port 8081
- LaunchDarkly mock server on port 8765

**Wait for all services to be healthy (this may take 1-2 minutes):**
```bash
# Check service health
docker-compose ps

# Wait for Schema Registry to be ready
while ! curl -f http://localhost:8081/subjects 2>/dev/null; do
  echo "Waiting for Schema Registry to start..."
  sleep 5
done
echo "✅ Schema Registry is ready!"

# Wait for LaunchDarkly mock server to be ready
while ! curl -f http://localhost:8765/status 2>/dev/null; do
  echo "Waiting for LaunchDarkly mock server to start..."
  sleep 5
done
echo "✅ LaunchDarkly mock server is ready!"
```

3. **Run the Flamingock migrations:**
```bash
./gradlew run
```

4. **Verify the results:**

Check MongoDB for the orders with discount fields:
```bash
docker exec -it inventory-mongodb mongosh inventory --eval 'db.orders.find().pretty()'
```

Check Schema Registry for the evolved schemas:
```bash
curl http://localhost:8081/subjects
curl http://localhost:8081/subjects/order-created-value/versions
```

Check the audit logs in MongoDB:
```bash
docker exec -it inventory-mongodb mongosh inventory --eval 'db.flamingockAuditLogs.find().pretty()'
```

5. **Clean up when done:**
```bash
docker-compose down -v
```

### Option 2: Run Tests

Run the integration tests with Testcontainers (no Docker Compose needed):
```bash
./gradlew test
```

### Option 3: Run with GraalVM Native Image (Optional)

If you want to showcase Flamingock running as a GraalVM native image, you can follow these **optional** steps. The regular JVM flow above still works as-is.

For full details, see the official docs: https://docs.flamingock.io/frameworks/graalvm

#### 1. Use a GraalVM Java distribution

Using SDKMAN:

```bash
sdk env install   # uses .sdkmanrc in this folder
sdk use java 17.0.8-graal  # or any installed GraalVM distribution compatible with your setup
```

The default `.sdkmanrc` included with this example already uses Java 17 with GraalVM.

#### 2. Ensure GraalVM support dependencies are present

This example already includes the Flamingock GraalVM integration and resource configuration:

- `build.gradle.kts` contains (commented):
   - `implementation("io.flamingock:flamingock-graalvm:$flamingockVersion")`
- `resource-config.json` in the project root includes:
   - `META-INF/flamingock/metadata.json` resources required at native-image time

If you copy this example to your own project, make sure you add the same pieces (or follow the docs linked above).

#### 3. Build the fat (uber) JAR

First build the application as usual, which also creates a **fat / uber JAR** bundling all runtime dependencies and a `Main-Class` manifest entry:

```bash
./gradlew clean build
```

The `jar` task in `build.gradle.kts` is configured like this:

```kotlin
tasks.named<Jar>("jar") {
   manifest {
      attributes["Main-Class"] = "io.flamingock.examples.inventory.InventoryOrdersApp"
   }

   duplicatesStrategy = DuplicatesStrategy.EXCLUDE

   from(sourceSets.main.get().output)

   from({
      configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
   })
}
```

This produces an executable uber JAR under `build/libs/` (for example `build/libs/inventory-orders-service-1.0-SNAPSHOT.jar`).

> **Why this matters for GraalVM**
>
> GraalVM's `native-image -jar` mode expects a JAR that:
> - has a valid `Main-Class` manifest attribute, and
> - contains all the classes and dependencies reachable from that entry point.
>
> The fat/uber JAR configuration above ensures those requirements are met, which is essential for the native-image step to work reliably.

#### 4. Create the native image

From the project root, run (adjust the JAR name and output binary name if needed):

```bash
native-image \
   --no-fallback \
   --features=io.flamingock.graalvm.RegistrationFeature \
   -H:ResourceConfigurationFiles=resource-config.json \
   -H:+ReportExceptionStackTraces \
   --initialize-at-build-time=org.slf4j.simple \
   -jar build/libs/inventory-orders-service-1.0-SNAPSHOT.jar \
   inventory-orders-service
```

This uses Flamingock's GraalVM feature to automatically register all required reflection metadata.

#### 5. Run the native image

With Docker Compose infrastructure already running (see [Option 1](#option-1-run-the-application-recommended)), start the native binary:

```bash
./inventory-orders-service
```

The application will execute the same Flamingock migrations as when running on the regular JVM, but with GraalVM-native startup and footprint characteristics.

## Troubleshooting

### Schema Registry Connection Issues

If you see connection errors to port 8081:

1. **Check if all services are healthy:**
```bash
docker-compose ps
```
All services should show "healthy" status.

2. **Check Schema Registry logs:**
```bash
docker logs inventory-schema-registry
```

3. **Restart services if needed:**
```bash
docker-compose down
docker-compose up -d
```

4. **Manual health check:**
```bash
# Test each service individually
curl http://localhost:8081/subjects    # Schema Registry
curl http://localhost:8765/status      # LaunchDarkly mock server
nc -zv localhost 9092                  # Kafka
nc -zv localhost 27017                 # MongoDB
```

### Common Issues

- **Schema Registry takes time to start**: Wait 1-2 minutes for full startup
- **Port conflicts**: Ensure ports 27017, 9092, 2181, 8081, 8765 are available
- **Docker resources**: Ensure Docker has sufficient memory (recommend 4GB+)

## Proven Functionalities

This example demonstrates the following Flamingock capabilities:

✅ **Multi-Target System Configuration** - Coordinating changes across MongoDB, Kafka, and LaunchDarkly Management API

✅ **Transactional vs Non-Transactional Changes** - MongoDB changes are transactional, while Kafka and LaunchDarkly API changes are non-transactional

✅ **Change-as-Code Pattern** - All system evolution is versioned and auditable through code

✅ **Schema Evolution** - Backward-compatible schema changes in Kafka

✅ **Feature Flag Lifecycle Management** - Creates flags for safe deployment, removes them when features become permanent

✅ **Audit Trail** - Complete history of all changes stored in MongoDB

✅ **Rollback Support** - Each change includes rollback logic for recovery

## Implemented Changes


| Deployment Step                | Change Name                                                                         | Target Systems        | Operation                         | Description                                                                                  |
|--------------------------------|-------------------------------------------------------------------------------------|-----------------------|-----------------------------------|----------------------------------------------------------------------------------------------|
| [Initial](#initial-deployment) | <a id="adddiscountcodefieldtoorders"></a>`AddDiscountCodeFieldToOrders`             | MongoDB               | Alter collection / add field      | Adds `discountCode` (nullable) to the orders collection                                      |
| [Initial](#initial-deployment) | <a id="updateordercreatedschema"></a>`UpdateOrderCreatedSchema`                     | Kafka Schema Registry | Register new schema version       | Publishes a new version of the OrderCreated event schema including discountCode              |
| [Initial](#initial-deployment) | <a id="addfeatureflagdiscounts"></a>`AddFeatureFlagDiscounts`                       | LaunchDarkly API       | Create flags                      | Creates feature flags for discount functionality using LaunchDarkly Management API           |
| [Second](#second-deployment)   | <a id="backfilldiscountsforexistingorders"></a>`BackfillDiscountsForExistingOrders` | MongoDB               | Update                            | Updates existing orders with discountCode = "NONE"                                           |
| [Second](#second-deployment)   | <a id="addindexondiscountcode"></a>`AddIndexOnDiscountCode`                         | MongoDB               | Create index                      | Creates an index on discountCode to support reporting and efficient lookups                  |
| [Final](#final-deployment)     | <a id="cleanupfeatureflagdiscounts"></a>`CleanupFeatureFlagDiscounts`               | LaunchDarkly API       | Archive flags                     | Archives temporary feature flags once the feature is permanent and code guards are removed  |
| [Final](#final-deployment)     | <a id="cleanupoldschemaversion"></a>`CleanupOldSchemaVersion`                       | Kafka Schema Registry | Disable/delete old schema version | Removes outdated schema once all consumers have migrated to the new version                  |

## Example Output

After running the migrations, you'll see:
- Orders in MongoDB with discount fields populated
- Two schema versions in Schema Registry (V1 and V2)
- LaunchDarkly Management API calls for feature flag creation/archival via mock server
- Complete audit trail in the flamingockAuditLogs collection

## Architecture Notes

The example uses:
- **MongoDB** as both a target system and the audit store
- **NonTransactionalTargetSystem** for Kafka and LaunchDarkly API changes
- **Utility classes** (KafkaSchemaManager, LaunchDarklyClient) for clean API abstractions
- **Staged execution** - All changes run in sequence to ensure consistency
- **HTTP REST calls** to LaunchDarkly Management API showing real integration patterns

---

## Contributing

We welcome contributions! If you have an idea for a new example or improvement to an existing one, feel free to submit a pull request. Check out our [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

---

## Get Involved

⭐ Star the [Flamingock repository](https://github.com/flamingock/flamingock-java) to show your support!

🐞 Report issues or suggest features in the [Flamingock issue tracker](https://github.com/flamingock/flamingock-java/issues).

💬 Join the discussion in the [Flamingock community](https://github.com/flamingock/flamingock-java/discussions).

---

## License

This repository is licensed under the [Apache License 2.0](../LICENSE.md).

---

## Explore, experiment, and empower your projects with Flamingock!

Let us know what you think or where you'd like to see Flamingock used next.
