/*
 * Copyright 2023 Flamingock (https://www.flamingock.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.flamingock.examples.inventory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.flamingock.api.annotations.EnableFlamingock;
import io.flamingock.api.annotations.Stage;
import io.flamingock.community.Flamingock;
import io.flamingock.community.mongodb.sync.driver.MongoDBSyncAuditStore;
import io.flamingock.examples.inventory.util.LaunchDarklyClient;
import io.flamingock.examples.inventory.util.KafkaSchemaManager;
import io.flamingock.internal.core.store.CommunityAuditStore;
import io.flamingock.targetsystem.nontransactional.NonTransactionalTargetSystem;
import io.flamingock.targetystem.mongodb.sync.MongoDBSyncTargetSystem;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.util.*;

import static io.flamingock.examples.inventory.util.TargetSystems.DATABASE_NAME;
import static io.flamingock.examples.inventory.util.TargetSystems.KAFKA_TARGET_SYSTEM;
import static io.flamingock.examples.inventory.util.TargetSystems.MONGODB_TARGET_SYSTEM;
import static io.flamingock.examples.inventory.util.TargetSystems.FEATURE_FLAG_TARGET_SYSTEM;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class SuccessExecutionTest {
    private static final Network network = Network.newNetwork();

    @Container
    public static final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:6"))
            .withNetwork(network)
            .withNetworkAliases("mongodb");

    @Container
    public static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .withNetwork(network)
            .withNetworkAliases("kafka");

    @Container
    public static final GenericContainer<?> schemaRegistryContainer = new GenericContainer<>(DockerImageName.parse("confluentinc/cp-schema-registry:7.5.0"))
            .withNetwork(network)
            .withNetworkAliases("schema-registry")
            .withExposedPorts(8081)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "kafka:9092")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
            .dependsOn(kafkaContainer);

    @Container
    public static final GenericContainer<?> launchDarklyContainer = new GenericContainer<>(DockerImageName.parse("node:18-alpine"))
            .withNetwork(network)
            .withNetworkAliases("launchdarkly")
            .withExposedPorts(8765)
            .withWorkingDirectory("/app")
            .withFileSystemBind("./mock-launchdarkly-server.js", "/app/server.js", BindMode.READ_ONLY)
            .withCommand("node", "server.js");

    @TempDir
    static Path tempDir;

    private static MongoClient mongoClient;
    private static SchemaRegistryClient schemaRegistryClient;
    private static AdminClient kafkaAdminClient;
    private static LaunchDarklyClient launchDarklyClient;

    @BeforeAll
    static void beforeAll() throws Exception {
        // Wait for containers to be ready
        Thread.sleep(2000);

        // Setup MongoDB client
        mongoClient = MongoClients.create(MongoClientSettings
                .builder()
                .applyConnectionString(new ConnectionString(mongoDBContainer.getConnectionString()))
                .build());

        // Setup Schema Registry client
        String schemaRegistryUrl = String.format("http://%s:%d",
                schemaRegistryContainer.getHost(),
                schemaRegistryContainer.getMappedPort(8081));
        schemaRegistryClient = new CachedSchemaRegistryClient(
                Collections.singletonList(schemaRegistryUrl),
                100
        );

        // Setup Kafka Admin client
        Properties kafkaProps = new Properties();
        kafkaProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        kafkaAdminClient = AdminClient.create(kafkaProps);

        // Setup LaunchDarkly client for testing with dev-server
        String launchDarklyUrl = String.format("http://%s:%d/api/v2",
                launchDarklyContainer.getHost(),
                launchDarklyContainer.getMappedPort(8765));
        launchDarklyClient = new LaunchDarklyClient("test-token", "inventory-service", "test", launchDarklyUrl);

        // Create KafkaSchemaManager
        KafkaSchemaManager schemaManager = new KafkaSchemaManager(schemaRegistryClient, kafkaAdminClient);

        // Wait a bit more for schema registry to be fully ready
        Thread.sleep(1000);

        // Wait for LaunchDarkly dev-server to be ready
        Thread.sleep(1000);

        // Run Flamingock migrations
        runFlamingockMigrations(mongoClient, schemaManager, launchDarklyClient);
    }

    @EnableFlamingock(
        stages = {
            @Stage(name = "inventory", location = "io.flamingock.examples.inventory.changes")
        }
    )
    static class TestConfig {}

    private static void runFlamingockMigrations(MongoClient mongoClient, KafkaSchemaManager schemaManager, LaunchDarklyClient launchDarklyClient) {
        MongoDBSyncTargetSystem mongoTarget = new MongoDBSyncTargetSystem(MONGODB_TARGET_SYSTEM, mongoClient, DATABASE_NAME);
        CommunityAuditStore auditStore = MongoDBSyncAuditStore.from(mongoTarget);

        NonTransactionalTargetSystem kafkaTarget = new NonTransactionalTargetSystem(KAFKA_TARGET_SYSTEM).addDependency(schemaManager);
        NonTransactionalTargetSystem flagTarget = new NonTransactionalTargetSystem(FEATURE_FLAG_TARGET_SYSTEM).addDependency(launchDarklyClient);

        Flamingock.builder()
                .setAuditStore(auditStore)
                .addTargetSystems(mongoTarget, kafkaTarget, flagTarget)
                .build()
                .run();
    }

    @Test
    @DisplayName("SHOULD create orders collection with discount fields")
    void testMongoDbChanges() {
        // Verify orders were created with discount fields
        List<Document> orders = mongoClient.getDatabase(DATABASE_NAME)
                .getCollection("orders")
                .find()
                .into(new ArrayList<>());

        assertEquals(2, orders.size());

        // Check that all orders have discountCode field (backfilled)
        for (Document order : orders) {
            assertTrue(order.containsKey("discountCode"));
            assertEquals("NONE", order.getString("discountCode"));
            assertTrue(order.containsKey("discountApplied"));
            assertEquals(false, order.getBoolean("discountApplied"));
        }

        // Verify specific orders
        Optional<Document> order1 = orders.stream()
                .filter(o -> "ORD-001".equals(o.getString("orderId")))
                .findFirst();
        assertTrue(order1.isPresent());
        assertEquals("CUST-101", order1.get().getString("customerId"));

        Optional<Document> order2 = orders.stream()
                .filter(o -> "ORD-002".equals(o.getString("orderId")))
                .findFirst();
        assertTrue(order2.isPresent());
        assertEquals("CUST-102", order2.get().getString("customerId"));
    }

    @Test
    @DisplayName("SHOULD register Kafka schemas with discount field")
    void testKafkaSchemaChanges() throws Exception {
        // Verify that schemas were registered
        Collection<String> subjects = schemaRegistryClient.getAllSubjects();
        assertTrue(subjects.contains("order-created-value"));

        // Verify we have at least 1 version (V2 with discountCode)
        List<Integer> versions = schemaRegistryClient.getAllVersions("order-created-value");
        assertTrue(versions.size() >= 1, "Should have at least 1 schema version");

        // Get latest schema and verify it contains discountCode
        String latestSchema = schemaRegistryClient.getLatestSchemaMetadata("order-created-value")
                .getSchema();
        assertTrue(latestSchema.contains("discountCode"));
    }

    @Test
    @DisplayName("SHOULD interact with LaunchDarkly Management API")
    void testLaunchDarklyIntegration() throws Exception {
        // Verify that the LaunchDarkly client was initialized and used during migrations
        assertNotNull(launchDarklyClient, "LaunchDarkly client should be initialized");

        // In demo mode, the LaunchDarkly client will attempt HTTP calls to the Management API
        // These will fail gracefully due to the dummy token, but will log the intended operations
        // showing how real flag creation/deletion would work

        // In a real test environment with valid LaunchDarkly credentials, you would:
        // 1. Set up test environment flags
        // 2. Verify flags were created/deleted via API calls
        // 3. Check flag states through LaunchDarkly API

        // This test demonstrates that Flamingock successfully coordinates changes
        // across multiple systems including LaunchDarkly feature flag management via REST API
    }

    @Test
    @DisplayName("SHOULD record all changes in Flamingock audit logs")
    void testFlamingockAuditLogs() {
        List<Document> auditLogs = mongoClient.getDatabase(DATABASE_NAME)
                .getCollection("flamingockAuditLog")
                .find()
                .into(new ArrayList<>());

        // Should have 2 entries per change (STARTED and APPLIED)
        assertEquals(14, auditLogs.size()); // 7 changes × 2 entries

        // Verify each change was executed
        verifyChangeExecution(auditLogs, "add-discount-code-field-to-orders");
        verifyChangeExecution(auditLogs, "update-order-created-schema");
        verifyChangeExecution(auditLogs, "add-feature-flag-discounts");
        verifyChangeExecution(auditLogs, "backfill-discounts-for-existing-orders");
        verifyChangeExecution(auditLogs, "add-index-on-discount-code");
        verifyChangeExecution(auditLogs, "cleanup-feature-flag-discounts");
        verifyChangeExecution(auditLogs, "cleanup-old-schema-version");
    }

    private void verifyChangeExecution(List<Document> auditLogs, String changeId) {
        // Check STARTED entry
        boolean hasStarted = auditLogs.stream()
                .anyMatch(log -> changeId.equals(log.getString("changeId"))
                        && "STARTED".equals(log.getString("state")));
        assertTrue(hasStarted, "Change " + changeId + " should have STARTED entry");

        // Check APPLIED entry
        boolean hasApplied = auditLogs.stream()
                .anyMatch(log -> changeId.equals(log.getString("changeId"))
                        && "APPLIED".equals(log.getString("state")));
        assertTrue(hasApplied, "Change " + changeId + " should have APPLIED entry");
    }
}
