package io.flamingock.examples.inventory;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.flamingock.internal.core.external.store.CommunityAuditStore;
import io.flamingock.store.mongodb.sync.MongoDBSyncAuditStore;
import io.flamingock.examples.inventory.util.KafkaSchemaManager;
import io.flamingock.examples.inventory.util.LaunchDarklyClient;
import io.flamingock.targetsystem.mongodb.springdata.MongoDBSpringDataTargetSystem;
import io.flamingock.targetsystem.nontransactional.NonTransactionalTargetSystem;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Configuration
public class FlamingockConfig {

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String kafkaBootstrapServers;

    @Value("${kafka.schema-registry-url:http://localhost:8081}")
    private String schemaRegistryUrl;

    @Value("${launchdarkly.api-url:http://localhost:8765/api/v2}")
    private String launchDarklyApiUrl;

    private AdminClient kafkaAdminClient;

    @Bean
    public MongoDBSpringDataTargetSystem mongoDBSpringDataTargetSystem(MongoTemplate mongoTemplate) {
        return new MongoDBSpringDataTargetSystem(TargetSystems.MONGODB_TARGET_SYSTEM, mongoTemplate);
    }

    @Bean
    public NonTransactionalTargetSystem kafkaTargetSystem() throws Exception {
        SchemaRegistryClient schemaRegistryClient = new CachedSchemaRegistryClient(
                Collections.singletonList(schemaRegistryUrl),
                100
        );

        Properties kafkaProps = new Properties();
        kafkaProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        this.kafkaAdminClient = AdminClient.create(kafkaProps);

        KafkaSchemaManager schemaManager = new KafkaSchemaManager(schemaRegistryClient, kafkaAdminClient);
        schemaManager.createTopicIfNotExists("order-created", 3, (short) 1);
        return new NonTransactionalTargetSystem(TargetSystems.KAFKA_TARGET_SYSTEM).addDependency(schemaManager);
    }

    @Bean
    public NonTransactionalTargetSystem toggleTargetSystem() {
        LaunchDarklyClient launchDarklyClient = new LaunchDarklyClient(
                "demo-token",
                "inventory-service",
                "production",
                launchDarklyApiUrl
        );
        return new NonTransactionalTargetSystem(TargetSystems.FEATURE_FLAG_TARGET_SYSTEM).addDependency(launchDarklyClient);
    }


    //This could return any of the available community audit stores
    @Bean
    public CommunityAuditStore auditStore(MongoDBSpringDataTargetSystem mongoDBSpringDataTargetSystem) {
        return MongoDBSyncAuditStore.from(mongoDBSpringDataTargetSystem);
    }

    @PreDestroy
    public void cleanup() {
        if (kafkaAdminClient != null) {
            kafkaAdminClient.close(Duration.ofSeconds(2));
        }
    }
}
