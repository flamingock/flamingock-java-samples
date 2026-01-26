package io.flamingock.examples.inventory;

import io.flamingock.examples.inventory.changes._0001__mongodb_addDiscountCodeFieldToOrders;
import io.flamingock.examples.inventory.changes._0002__kafka_updateOrderCreatedSchema;
import io.flamingock.examples.inventory.changes._0003__toggle_addFeatureFlagDiscounts;
import io.flamingock.examples.inventory.changes._0004__mongodb_backfillDiscountsForExistingOrders;
import io.flamingock.examples.inventory.changes._0005__mongodb_addIndexOnDiscountCode;
import io.flamingock.examples.inventory.changes._0006__toggle_cleanupFeatureFlagDiscounts;
import io.flamingock.examples.inventory.changes._0007__kafka_leanupOldSchemaVersion;
import io.flamingock.springboot.testsupport.FlamingockSpringBootTest;
import io.flamingock.springboot.testsupport.FlamingockSpringBootTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static io.flamingock.support.domain.AuditEntryDefinition.APPLIED;

@FlamingockSpringBootTest(classes = InventoryOrdersApp.class)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InventoryOrdersAppTest {

    static final Network network = Network.newNetwork();

    @Autowired
    public FlamingockSpringBootTestSupport flamingockTestSupport;

    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:6"))
            .withNetwork(network)
            .withNetworkAliases("mongodb");

    @Container
    static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .withNetwork(network)
            .withNetworkAliases("kafka");

    @Container
    static final GenericContainer<?> schemaRegistryContainer = new GenericContainer<>(DockerImageName.parse("confluentinc/cp-schema-registry:7.5.0"))
            .withNetwork(network)
            .withNetworkAliases("schema-registry")
            .withExposedPorts(8081)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "kafka:9092")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
            .dependsOn(kafkaContainer);

    @Container
    static final GenericContainer<?> launchDarklyContainer = new GenericContainer<>(DockerImageName.parse("node:18-alpine"))
            .withNetwork(network)
            .withNetworkAliases("launchdarkly")
            .withExposedPorts(8765)
            .withWorkingDirectory("/app")
            .withFileSystemBind("./mock-launchdarkly-server.js", "/app/server.js", BindMode.READ_ONLY)
            .withCommand("node", "server.js");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("kafka.schema-registry-url", () -> String.format("http://%s:%d",
                schemaRegistryContainer.getHost(),
                schemaRegistryContainer.getMappedPort(8081)));
        registry.add("launchdarkly.api-url", () -> String.format("http://%s:%d/api/v2",
                launchDarklyContainer.getHost(),
                launchDarklyContainer.getMappedPort(8765)));
    }



    @Test
    void allChangesAppliedSuccessfully() {
        flamingockTestSupport
                .givenBuilderFromContext()
                .whenRun()
                .thenExpectAuditFinalStateSequence(
                        APPLIED(_0001__mongodb_addDiscountCodeFieldToOrders.class),
                        APPLIED(_0002__kafka_updateOrderCreatedSchema.class),
                        APPLIED(_0003__toggle_addFeatureFlagDiscounts.class),
                        APPLIED(_0004__mongodb_backfillDiscountsForExistingOrders.class),
                        APPLIED(_0005__mongodb_addIndexOnDiscountCode.class),
                        APPLIED(_0006__toggle_cleanupFeatureFlagDiscounts.class),
                        APPLIED(_0007__kafka_leanupOldSchemaVersion.class)
                )
                .verify();
    }
}
