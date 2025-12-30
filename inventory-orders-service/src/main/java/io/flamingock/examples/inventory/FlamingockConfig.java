package io.flamingock.examples.inventory;

import com.mongodb.client.MongoClient;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.flamingock.store.mongodb.sync.MongoDBSyncAuditStore;
import io.flamingock.examples.inventory.util.KafkaSchemaManager;
import io.flamingock.examples.inventory.util.LaunchDarklyClient;
import io.flamingock.examples.inventory.util.MongoDBUtil;
import io.flamingock.internal.core.store.CommunityAuditStore;
import io.flamingock.targetsystem.nontransactional.NonTransactionalTargetSystem;
import io.flamingock.targetystem.mongodb.sync.MongoDBSyncTargetSystem;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Configuration
public class FlamingockConfig {

    @Value("${mongodb.uri:mongodb://localhost:27017/}")
    private String mongodbUri;

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String kafkaBootstrapServers;

    @Value("${kafka.schema-registry-url:http://localhost:8081}")
    private String schemaRegistryUrl;

    @Value("${launchdarkly.api-url:http://localhost:8765/api/v2}")
    private String launchDarklyApiUrl;

    private AdminClient kafkaAdminClient;

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient() {
        return MongoDBUtil.getMongoClient(mongodbUri);
    }

    @Bean
    public MongoDBSyncTargetSystem mongoDBSyncTargetSystem(MongoClient mongoClient) {
        return new MongoDBSyncTargetSystem(TargetSystems.MONGODB_TARGET_SYSTEM, mongoClient, TargetSystems.DATABASE_NAME);
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
    public CommunityAuditStore auditStore(MongoDBSyncTargetSystem mongoDBSyncTargetSystem) {
        return MongoDBSyncAuditStore.from(mongoDBSyncTargetSystem);
    }

    @PreDestroy
    public void cleanup() {
        if (kafkaAdminClient != null) {
            kafkaAdminClient.close(Duration.ofSeconds(2));
        }
    }
}
